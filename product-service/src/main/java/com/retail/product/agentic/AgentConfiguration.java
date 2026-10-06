package com.retail.product.agentic;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.retail.product.service.RecommendationAgent;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.service.AiServices;

@Configuration
public class AgentConfiguration {

    @Bean
    @SuppressWarnings("unused")
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .build();
    }

    @Bean
    @SuppressWarnings("unused")
    ChatModelListener chatModelLogger() {
        return new ModelLogger();
    }

    @Bean
    @SuppressWarnings("unused")
    dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<dev.langchain4j.data.segment.TextSegment> inMemoryEmbeddingStore() {
        return new dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<>();
    }

    @Bean
    @SuppressWarnings("unused")
    org.springframework.boot.CommandLineRunner ingestProductsOnStartup(com.retail.product.agentic.LangChain4jProductIngestor ingestor) {
        return args -> {
            try {
                ingestor.ingestAll();
            } catch (Exception ex) {
                LoggerFactory.getLogger(AgentConfiguration.class).warn("Failed to ingest products on startup", ex);
            }
        };
    }

    @Bean
    public Cache<String, float[]> embeddingCache(
            @org.springframework.beans.factory.annotation.Value("${embedding.cache.max-size:10000}") int maxSize,
            @org.springframework.beans.factory.annotation.Value("${embedding.cache.expire-hours:24}") int expireHours) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofHours(expireHours))
                .build();
    }

    // Lazy + ObjectProvider so the ChatModel lookup happens on first use, after Ollama/Gemini
    // auto-configuration (which runs after this component-scanned @Configuration) has created its bean.
    @Bean
    @Lazy
    @ConditionalOnProperty(name = "retail.ai.recommendation.enabled", havingValue = "true")
    RecommendationAgent recommendationAgent(ObjectProvider<ChatModel> chatModelProvider,
            ChatMemoryProvider chatMemoryProvider, Tools tools, Environment env,
            dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<dev.langchain4j.data.segment.TextSegment> store,
            java.util.Optional<dev.langchain4j.model.embedding.EmbeddingModel> embeddingModelOpt) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            // Return a delegating agent that will retry initialization when a ChatModel becomes available.
            // This keeps the RecommendationAgent bean present so controllers can attempt requests while
            // the model provider (e.g. Ollama) starts up, and it will auto-initialize when possible.
            return new RetryingRecommendationAgent(chatModelProvider, chatMemoryProvider, tools, env, store, embeddingModelOpt);
        }
        Logger log = LoggerFactory.getLogger(AgentConfiguration.class);
        // Push recommended model tuning into system properties so provider starters can pick them up
        String temp = env.getProperty("ai.model.temperature");
        String maxOut = env.getProperty("ai.model.max-output-tokens");
        if (temp != null) {
            System.setProperty("ai.model.temperature", temp);
            log.info("Set ai.model.temperature={} from environment", temp);
        }
        if (maxOut != null) {
            System.setProperty("ai.model.max-output-tokens", maxOut);
            log.info("Set ai.model.max-output-tokens={} from environment", maxOut);
        }
        // Build the AiServices builder so we can optionally attach a ContentRetriever at runtime
        var builder = AiServices.builder(RecommendationAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(tools);

        // Try to wire a runtime ContentRetriever backed by our in-memory embedding store.
        // This is done reflectively so the code compiles even if the retriever API is not present.
        try {
            // Look for a retriever type in the langchain4j package
            Class<?> retrieverIfc = null;
            try {
                retrieverIfc = Class.forName("dev.langchain4j.retriever.ContentRetriever");
            } catch (ClassNotFoundException ignore) {
                try {
                    retrieverIfc = Class.forName("dev.langchain4j.retriever.Retriever");
                } catch (ClassNotFoundException ignore2) {
                    // no retriever API available on classpath; skip wiring
                }
            }

            final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel = embeddingModelOpt.orElse(null);
            if (retrieverIfc != null) {
                // Create a dynamic proxy implementing the retriever interface.
                // The proxy will accept a query as either a String or a TextSegment and return a List of TextSegment
                java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                    // Accept any retrieve-like method and return matching TextSegments from the embedding store
                    try {
                        String query = null;
                        if (args != null && args.length > 0) {
                            Object a0 = args[0];
                            if (a0 instanceof String s) {
                                query = s;
                            } else if (a0 != null) {
                                // try to call text() if it's a TextSegment-like object
                                try {
                                    java.lang.reflect.Method textM = a0.getClass().getMethod("text");
                                    Object txt = textM.invoke(a0);
                                    if (txt instanceof String ts) {
                                        query = ts;
                                    }
                                } catch (NoSuchMethodException ignored) {
                                }
                            }
                        }
                        if (query == null) {
                            return java.util.List.of();
                        }
                        // Build an embedding for the query using the configured EmbeddingModel if available,
                        // otherwise fall back to the deterministic placeholder embedding.
                        dev.langchain4j.data.embedding.Embedding qEmb = null;
                        try {
                            if (embeddingModel != null) {
                                dev.langchain4j.data.segment.TextSegment qSeg = dev.langchain4j.data.segment.TextSegment.from(query);
                                dev.langchain4j.model.output.Response<dev.langchain4j.data.embedding.Embedding> resp = embeddingModel.embed(qSeg);
                                if (resp != null) {
                                    qEmb = resp.content();
                                }
                            }
                        } catch (Throwable ignore) {
                        }
                        if (qEmb == null) {
                            qEmb = createPlaceholderEmbedding(query);
                        }
                        dev.langchain4j.store.embedding.EmbeddingSearchRequest req = new dev.langchain4j.store.embedding.EmbeddingSearchRequest(qEmb, 10, 0.5, null);
                        dev.langchain4j.store.embedding.EmbeddingSearchResult<dev.langchain4j.data.segment.TextSegment> res = store.search(req);
                        java.util.List<dev.langchain4j.data.segment.TextSegment> out = new java.util.ArrayList<>();
                        for (var m : res.matches()) {
                            try {
                                // try to extract id from the match (id() or getId())
                                String idStr = null;
                                try {
                                    java.lang.reflect.Method idM = m.getClass().getMethod("id");
                                    Object val = idM.invoke(m);
                                    if (val != null) {
                                        idStr = String.valueOf(val);
                                    }
                                } catch (NoSuchMethodException ignored) {
                                    try {
                                        java.lang.reflect.Method getId = m.getClass().getMethod("getId");
                                        Object val = getId.invoke(m);
                                        if (val != null) {
                                            idStr = String.valueOf(val);
                                        }
                                    } catch (NoSuchMethodException ignored2) {
                                        // no id method
                                    }
                                }

                                // try to extract embedded TextSegment
                                dev.langchain4j.data.segment.TextSegment tsFound = null;
                                try {
                                    java.lang.reflect.Method embeddedM = m.getClass().getMethod("embedded");
                                    Object embedded = embeddedM.invoke(m);
                                    if (embedded instanceof dev.langchain4j.data.segment.TextSegment ts) {
                                        tsFound = ts;
                                    }
                                } catch (NoSuchMethodException ignored) {
                                    try {
                                        java.lang.reflect.Method getEmbedded = m.getClass().getMethod("getEmbedded");
                                        Object embedded = getEmbedded.invoke(m);
                                        if (embedded instanceof dev.langchain4j.data.segment.TextSegment ts) {
                                            tsFound = ts;
                                        }
                                    } catch (NoSuchMethodException ignored2) {
                                        // nothing we can do
                                    }
                                }

                                if (tsFound != null) {
                                    // prefix the segment text with the product id when available
                                    String prefix = idStr == null ? "" : ("ID=" + idStr + "\n");
                                    dev.langchain4j.data.segment.TextSegment outSeg = dev.langchain4j.data.segment.TextSegment.from(prefix + tsFound.text());
                                    out.add(outSeg);
                                } else if (idStr != null) {
                                    out.add(dev.langchain4j.data.segment.TextSegment.from("ID=" + idStr));
                                }
                            } catch (Throwable t) {
                                // ignore this match
                            }
                        }
                        return out;
                    } catch (Throwable t) {
                        return java.util.List.of();
                    }
                };

                Object retrieverProxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{retrieverIfc}, handler);

                // Find a suitable method on the builder to attach the retriever (names vary by langchain4j versions)
                java.lang.reflect.Method attachMethod = null;
                for (String name : new String[]{"retriever", "contentRetriever", "withRetriever"}) {
                    try {
                        attachMethod = builder.getClass().getMethod(name, retrieverIfc);
                        break;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                if (attachMethod != null) {
                    attachMethod.invoke(builder, retrieverProxy);
                    LoggerFactory.getLogger(AgentConfiguration.class).info("Wired ContentRetriever into AiServices builder");
                }
            }
        } catch (Throwable ignore) {
            // Non-fatal: if anything goes wrong, we simply don't wire a retriever
            LoggerFactory.getLogger(AgentConfiguration.class).debug("ContentRetriever wiring skipped: {}", ignore == null ? "none" : ignore.getMessage());
        }

        return builder.build();
    }

    private static dev.langchain4j.data.embedding.Embedding createPlaceholderEmbedding(String content) {
        int dim = 256;
        float[] vec = new float[dim];
        int h = content == null ? 0 : content.hashCode();
        for (int i = 0; i < dim; i++) {
            vec[i] = ((h >> (i % 32)) & 0xFF) / 255.0f;
        }
        return dev.langchain4j.data.embedding.Embedding.from(vec);
    }

    // Provide a local AllMiniLmL6V2 embedding model when no other EmbeddingModel bean
    // (e.g. from the Gemini or Ollama starters) has been registered.
    @Bean
    @Lazy
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(EmbeddingModel.class)
    EmbeddingModel localEmbeddingModel() {
        Logger log = LoggerFactory.getLogger(AgentConfiguration.class);
        try {
            EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();
            log.info("Using local AllMiniLmL6V2EmbeddingModel for product embeddings");
            return model;
        } catch (Throwable t) {
            log.warn("Failed to instantiate AllMiniLmL6V2EmbeddingModel: {}", t.getMessage());
            throw t instanceof RuntimeException re ? re : new RuntimeException(t);
        }
    }
}
