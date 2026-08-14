package com.financialhub.infrastructure.favorites;

import com.financialhub.application.port.out.FavoriteStorePort;
import com.financialhub.domain.model.FavoritePayee;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.favorites.store", havingValue = "mongo")
@RequiredArgsConstructor
public class MongoFavoriteStoreAdapter implements FavoriteStorePort {

    static final String COLLECTION = "favorites";
    private static final String OWNER_DOCUMENT = "ownerDocument";
    private static final String PAYEE_DOCUMENT = "payeeDocument";

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    void indexes() {
        mongoTemplate.getCollection(COLLECTION).createIndex(
                Indexes.compoundIndex(Indexes.ascending(OWNER_DOCUMENT), Indexes.ascending(PAYEE_DOCUMENT)),
                new IndexOptions().unique(true)
        );
    }

    @Override
    public List<FavoritePayee> list(String ownerDocument) {
        return mongoTemplate.find(byOwner(ownerDocument), FavoriteDocument.class, COLLECTION).stream()
                .map(FavoriteDocument::toModel)
                .sorted(Comparator.comparing(FavoritePayee::savedAt).reversed())
                .toList();
    }

    @Override
    public List<FavoritePayee> add(String ownerDocument, FavoritePayee favorite) {
        Query existing = byOwner(ownerDocument)
                .addCriteria(Criteria.where(PAYEE_DOCUMENT).is(favorite.document()));
        mongoTemplate.remove(existing, COLLECTION);
        mongoTemplate.insert(FavoriteDocument.from(ownerDocument, favorite), COLLECTION);
        return list(ownerDocument).stream().limit(30).toList();
    }

    @Override
    public List<FavoritePayee> remove(String ownerDocument, String payeeDocument) {
        Query query = byOwner(ownerDocument)
                .addCriteria(Criteria.where(PAYEE_DOCUMENT).is(payeeDocument));
        mongoTemplate.remove(query, COLLECTION);
        return list(ownerDocument);
    }

    private static Query byOwner(String ownerDocument) {
        return new Query(Criteria.where(OWNER_DOCUMENT).is(ownerDocument));
    }

    @Document(collection = COLLECTION)
    public record FavoriteDocument(
            String ownerDocument,
            String payeeDocument,
            String name,
            Instant savedAt
    ) {
        static FavoriteDocument from(String owner, FavoritePayee favorite) {
            return new FavoriteDocument(owner, favorite.document(), favorite.name(), favorite.savedAt());
        }

        FavoritePayee toModel() {
            return new FavoritePayee(payeeDocument, name, savedAt);
        }
    }
}
