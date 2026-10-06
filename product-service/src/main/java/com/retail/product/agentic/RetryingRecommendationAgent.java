package com.retail.product.agentic;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import com.retail.product.service.RecommendationAgent;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;

/**
 * Delegating RecommendationAgent that retries constructing a real agent when a ChatModel
 * bean becomes available. Intended to be a small, safe resilience layer so the application
 * can start even if the model provider (Ollama/Gemini) starts later.
 */
public class RetryingRecommendationAgent implements RecommendationAgent {

    private static final Logger log = LoggerFactory.getLogger(RetryingRecommendationAgent.class);

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ChatMemoryProvider chatMemoryProvider;
    private final Tools tools;
    private final Environment env;
    private final dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<dev.langchain4j.data.segment.TextSegment> store;
    private final Optional<EmbeddingModel> embeddingModelOpt;

    // Volatile delegate updated when real agent is built
    private volatile RecommendationAgent delegate;
    private final ScheduledExecutorService scheduler;

    public RetryingRecommendationAgent(ObjectProvider<ChatModel> chatModelProvider,
                                       ChatMemoryProvider chatMemoryProvider,
                                       Tools tools,
                                       Environment env,
                                       dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<dev.langchain4j.data.segment.TextSegment> store,
                                       Optional<EmbeddingModel> embeddingModelOpt) {
        this.chatModelProvider = chatModelProvider;
        this.chatMemoryProvider = chatMemoryProvider;
        this.tools = tools;
        this.env = env;
        this.store = store;
        this.embeddingModelOpt = embeddingModelOpt;

        // Start background scheduler to attempt initialization periodically
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "retrying-recommendation-agent-init");
            t.setDaemon(true);
            return t;
        });
        // Try quickly and then every 5 seconds
        this.scheduler.scheduleWithFixedDelay(this::initIfPossible, 0, 5, TimeUnit.SECONDS);
    }

    private void initIfPossible() {
        try {
            if (delegate != null) return;
            ChatModel chatModel = chatModelProvider.getIfAvailable();
            if (chatModel == null) return;

            Logger log = LoggerFactory.getLogger(RetryingRecommendationAgent.class);
            // Propagate environment tuning into system properties like original configuration
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

            var builder = AiServices.builder(RecommendationAgent.class)
                    .chatModel(chatModel)
                    .chatMemoryProvider(chatMemoryProvider)
                    .tools(tools);

            // Attempt to wire retriever reflectively (copied from AgentConfiguration)
            try {
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
                final EmbeddingModel embeddingModel = embeddingModelOpt.orElse(null);
                if (retrieverIfc != null) {
                    java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                        try {
                            String query = null;
                            if (args != null && args.length > 0) {
                                Object a0 = args[0];
                                if (a0 instanceof String s) query = s;
                                else if (a0 != null) {
                                    try {
                                        java.lang.reflect.Method textM = a0.getClass().getMethod("text");
                                        Object txt = textM.invoke(a0);
                                        if (txt instanceof String ts) query = ts;
                                    } catch (NoSuchMethodException ignored) {
                                    }
                                }
                            }
                            if (query == null) return java.util.List.of();

                            dev.langchain4j.data.embedding.Embedding qEmb = null;
                            try {
                                if (embeddingModel != null) {
                                    dev.langchain4j.data.segment.TextSegment qSeg = dev.langchain4j.data.segment.TextSegment.from(query);
                                    dev.langchain4j.model.output.Response<dev.langchain4j.data.embedding.Embedding> resp = embeddingModel.embed(qSeg);
                                    if (resp != null) qEmb = resp.content();
                                }
                            } catch (Throwable ignore) {}
                            if (qEmb == null) qEmb = createPlaceholderEmbedding(query);

                            dev.langchain4j.store.embedding.EmbeddingSearchRequest req = new dev.langchain4j.store.embedding.EmbeddingSearchRequest(qEmb, 10, 0.5, null);
                            dev.langchain4j.store.embedding.EmbeddingSearchResult<dev.langchain4j.data.segment.TextSegment> res = store.search(req);
                            java.util.List<dev.langchain4j.data.segment.TextSegment> out = new java.util.ArrayList<>();
                            for (var m : res.matches()) {
                                try {
                                    String idStr = null;
                                    try {
                                        java.lang.reflect.Method idM = m.getClass().getMethod("id");
                                        Object val = idM.invoke(m);
                                        if (val != null) idStr = String.valueOf(val);
                                    } catch (NoSuchMethodException ignored) {
                                        try {
                                            java.lang.reflect.Method getId = m.getClass().getMethod("getId");
                                            Object val = getId.invoke(m);
                                            if (val != null) idStr = String.valueOf(val);
                                        } catch (NoSuchMethodException ignored2) {
                                        }
                                    }
                                    dev.langchain4j.data.segment.TextSegment tsFound = null;
                                    try {
                                        java.lang.reflect.Method embeddedM = m.getClass().getMethod("embedded");
                                        Object embedded = embeddedM.invoke(m);
                                        if (embedded instanceof dev.langchain4j.data.segment.TextSegment ts) tsFound = ts;
                                    } catch (NoSuchMethodException ignored) {
                                        try {
                                            java.lang.reflect.Method getEmbedded = m.getClass().getMethod("getEmbedded");
                                            Object embedded = getEmbedded.invoke(m);
                                            if (embedded instanceof dev.langchain4j.data.segment.TextSegment ts) tsFound = ts;
                                        } catch (NoSuchMethodException ignored2) {}
                                    }
                                    if (tsFound != null) {
                                        String prefix = idStr == null ? "" : ("ID=" + idStr + "\n");
                                        dev.langchain4j.data.segment.TextSegment outSeg = dev.langchain4j.data.segment.TextSegment.from(prefix + tsFound.text());
                                        out.add(outSeg);
                                    } else if (idStr != null) {
                                        out.add(dev.langchain4j.data.segment.TextSegment.from("ID=" + idStr));
                                    }
                                } catch (Throwable t) {}
                            }
                            return out;
                        } catch (Throwable t) {
                            return java.util.List.of();
                        }
                    };

                    Object retrieverProxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{retrieverIfc}, handler);
                    java.lang.reflect.Method attachMethod = null;
                    for (String name : new String[]{"retriever", "contentRetriever", "withRetriever"}) {
                        try {
                            attachMethod = builder.getClass().getMethod(name, retrieverIfc);
                            break;
                        } catch (NoSuchMethodException ignored) {}
                    }
                    if (attachMethod != null) {
                        attachMethod.invoke(builder, retrieverProxy);
                        log.info("Wired ContentRetriever into AiServices builder");
                    }
                }
            } catch (Throwable ignore) {
                log.debug("ContentRetriever wiring skipped: {}", ignore == null ? "none" : ignore.getMessage());
            }

            RecommendationAgent built = builder.build();
            this.delegate = built;
            log.info("RecommendationAgent initialized and ready (delegate={})", built.getClass().getName());
            // Additional readiness info for ops: include a readable marker so logs can be grepped
            log.info("RetryingRecommendationAgent: real RecommendationAgent is ready and accepting requests");

            // Once initialized, shut down scheduler
            this.scheduler.shutdown();
        } catch (Throwable t) {
            // keep retrying
            log.debug("RetryingRecommendationAgent initialization attempt failed: {}", t.getMessage());
        }
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

    @Override
    public String recommend(String sessionId, String message) {
        // Try a synchronous init path first to reduce latency when Ollama is starting concurrently
        for (int i = 0; i < 5; i++) {
            if (delegate != null) break;
            initIfPossible();
            if (delegate != null) break;
            try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        RecommendationAgent d = this.delegate;
        if (d == null) {
            log.info("RecommendationAgent not ready yet (model provider unavailable)");
            throw new RuntimeException("RecommendationAgent not ready");
        }
        return d.recommend(sessionId, message);
    }
}
