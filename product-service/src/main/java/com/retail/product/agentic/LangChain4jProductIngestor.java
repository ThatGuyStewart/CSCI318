package com.retail.product.agentic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.retail.product.domain.ProductView;
import com.retail.product.repository.ProductViewRepository;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.Optional;

@Component
public class LangChain4jProductIngestor {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jProductIngestor.class);

    private final ProductViewRepository productViewRepository;
    private final InMemoryEmbeddingStore<TextSegment> store;
    private final Optional<EmbeddingModel> embeddingModelOpt;

    public LangChain4jProductIngestor(ProductViewRepository productViewRepository,
            InMemoryEmbeddingStore<TextSegment> store,
            Optional<EmbeddingModel> embeddingModelOpt) {
        this.productViewRepository = productViewRepository;
        this.store = store;
        this.embeddingModelOpt = embeddingModelOpt;
    }

    public void ingestAll() {
        try {
            var all = productViewRepository.findAll();
            log.info("Ingesting {} products into embedding store", all.size());
            for (ProductView p : all) {
                try {
                    String text = buildText(p);
                    dev.langchain4j.data.document.Metadata meta = new dev.langchain4j.data.document.Metadata();
                    meta.put("productId", String.valueOf(p.getProductId()));
                    TextSegment ts = TextSegment.from(text, meta);
                    Embedding emb = createEmbedding(text);
                    store.add(String.valueOf(p.getProductId()), emb, ts);
                } catch (Throwable t) {
                    log.debug("Failed to ingest product {}: {}", p.getProductId(), t.getMessage());
                }
            }
        } catch (Throwable t) {
            log.warn("Product ingestion failed: {}", t.getMessage());
        }
    }

    private String buildText(ProductView p) {
        StringBuilder sb = new StringBuilder();
        if (p.getName() != null) {
            sb.append(p.getName()).append("\n");
        }
        if (p.getCategory() != null) {
            sb.append(p.getCategory().name()).append("\n");
        }
        if (p.getDescription() != null) {
            sb.append(p.getDescription()).append("\n");
        }
        return sb.toString();
    }

    private Embedding createEmbedding(String text) {
        try {
            if (embeddingModelOpt != null && embeddingModelOpt.isPresent()) {
                EmbeddingModel m = embeddingModelOpt.get();
                Response<Embedding> resp = m.embed(text);
                if (resp != null && resp.content() != null) {
                    return resp.content();
                }
            }
        } catch (Throwable ignore) {
        }
        return createPlaceholderEmbedding(text);
    }

    private Embedding createPlaceholderEmbedding(String content) {
        int dim = 256;
        float[] vec = new float[dim];
        int h = content == null ? 0 : content.hashCode();
        for (int i = 0; i < dim; i++) {
            vec[i] = ((h >> (i % 32)) & 0xFF) / 255.0f;
        }
        return Embedding.from(vec);
    }
}
