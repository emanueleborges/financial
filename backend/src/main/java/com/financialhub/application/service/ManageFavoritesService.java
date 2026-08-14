package com.financialhub.application.service;

import com.financialhub.application.port.in.ManageFavoritesUseCase;
import com.financialhub.application.port.out.FavoriteStorePort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.exception.DomainException;
import com.financialhub.domain.exception.InvalidTransactionException;
import com.financialhub.domain.exception.UserNotFoundException;
import com.financialhub.domain.model.FavoritePayee;
import com.financialhub.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManageFavoritesService implements ManageFavoritesUseCase {

    private final FavoriteStorePort favoriteStore;
    private final UserRepositoryPort userRepository;

    @Override
    public List<FavoritePayee> list(Command command) {
        String owner = assertOwner(command.ownerDocument(), command.requesterDocument());
        assertUserExists(owner);
        return favoriteStore.list(owner);
    }

    @Override
    public List<FavoritePayee> add(AddCommand command) {
        String owner = assertOwner(command.ownerDocument(), command.requesterDocument());
        assertUserExists(owner);

        String payee = normalize(command.payeeDocument());
        if (payee.equals(owner)) {
            throw new InvalidTransactionException("Não é possível favoritar o próprio CPF/CNPJ");
        }
        if (!isValidDocumentLength(payee)) {
            throw new InvalidTransactionException("CPF/CNPJ inválido");
        }

        User payeeUser = userRepository.findByDocument(payee)
                .orElseThrow(() -> new UserNotFoundException("documento " + payee));

        String name = command.name() == null || command.name().isBlank()
                ? payeeUser.getName()
                : command.name().trim();

        return favoriteStore.add(owner, new FavoritePayee(payee, name, Instant.now()));
    }

    @Override
    public List<FavoritePayee> remove(RemoveCommand command) {
        String owner = assertOwner(command.ownerDocument(), command.requesterDocument());
        assertUserExists(owner);
        return favoriteStore.remove(owner, normalize(command.payeeDocument()));
    }

    private String assertOwner(String ownerDocument, String requesterDocument) {
        String owner = normalize(ownerDocument);
        String requester = normalize(requesterDocument);
        if (!owner.equals(requester)) {
            throw new DomainException("FORBIDDEN", "Só é possível gerenciar os próprios favoritos");
        }
        return owner;
    }

    private void assertUserExists(String document) {
        userRepository.findByDocument(document)
                .orElseThrow(() -> new UserNotFoundException("documento " + document));
    }

    private static String normalize(String document) {
        return document == null ? "" : document.replaceAll("\\D", "");
    }

    private static boolean isValidDocumentLength(String digits) {
        return digits.length() == 11 || digits.length() == 14;
    }
}
