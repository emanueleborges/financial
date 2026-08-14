package com.financialhub.infrastructure.favorites;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@ConditionalOnProperty(name = "app.favorites.store", havingValue = "mongo")
public class MongoFavoritesConfig {

    @Bean
    public MongoClient mongoClient(@Value("${app.mongodb.uri}") String uri) {
        return MongoClients.create(uri);
    }

    @Bean
    public MongoTemplate mongoTemplate(
            MongoClient mongoClient,
            @Value("${app.mongodb.database}") String database) {
        return new MongoTemplate(mongoClient, database);
    }
}
