package com.financialhub.infrastructure.favorites;

import com.financialhub.application.port.out.FavoriteStorePort;
import com.financialhub.domain.model.FavoritePayee;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.favorites.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryFavoriteStoreAdapter implements FavoriteStorePort {

    private final Map<String, List<FavoritePayee>> store = new ConcurrentHashMap<>();

    @Override
    public List<FavoritePayee> list(String ownerDocument) {
        return sortedCopy(store.getOrDefault(ownerDocument, List.of()));
    }

    @Override
    public List<FavoritePayee> add(String ownerDocument, FavoritePayee favorite) {
        List<FavoritePayee> current = new ArrayList<>(
                store.getOrDefault(ownerDocument, List.of()).stream()
                        .filter(item -> !item.document().equals(favorite.document()))
                        .toList()
        );
        current.add(0, favorite);
        List<FavoritePayee> next = current.stream().limit(30).toList();
        store.put(ownerDocument, next);
        return sortedCopy(next);
    }

    @Override
    public List<FavoritePayee> remove(String ownerDocument, String payeeDocument) {
        List<FavoritePayee> next = store.getOrDefault(ownerDocument, List.of()).stream()
                .filter(item -> !item.document().equals(payeeDocument))
                .toList();
        store.put(ownerDocument, next);
        return sortedCopy(next);
    }

    private static List<FavoritePayee> sortedCopy(List<FavoritePayee> items) {
        return items.stream()
                .sorted(Comparator.comparing(FavoritePayee::savedAt).reversed())
                .toList();
    }
}
