package com.financialhub.application.port.in;

import com.financialhub.domain.model.FavoritePayee;

import java.util.List;

public interface ManageFavoritesUseCase {

    List<FavoritePayee> list(Command command);

    List<FavoritePayee> add(AddCommand command);

    List<FavoritePayee> remove(RemoveCommand command);

    record Command(String ownerDocument, String requesterDocument) {}

    record AddCommand(String ownerDocument, String requesterDocument, String payeeDocument, String name) {}

    record RemoveCommand(String ownerDocument, String requesterDocument, String payeeDocument) {}
}
