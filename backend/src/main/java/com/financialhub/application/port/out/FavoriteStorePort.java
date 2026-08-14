package com.financialhub.application.port.out;

import com.financialhub.domain.model.FavoritePayee;

import java.util.List;

public interface FavoriteStorePort {

    List<FavoritePayee> list(String ownerDocument);

    List<FavoritePayee> add(String ownerDocument, FavoritePayee favorite);

    List<FavoritePayee> remove(String ownerDocument, String payeeDocument);
}
