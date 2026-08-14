package com.financialhub.application;

import com.financialhub.application.port.out.FavoriteStorePort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.application.service.ManageFavoritesService;
import com.financialhub.domain.enums.UserStatus;
import com.financialhub.domain.exception.DomainException;
import com.financialhub.domain.exception.InvalidTransactionException;
import com.financialhub.domain.exception.UserNotFoundException;
import com.financialhub.domain.model.FavoritePayee;
import com.financialhub.domain.model.User;
import com.financialhub.infrastructure.favorites.InMemoryFavoriteStoreAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageFavoritesServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    private FavoriteStorePort store;
    private ManageFavoritesService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryFavoriteStoreAdapter();
        service = new ManageFavoritesService(store, userRepository);
    }

    @Test
    void br015_addFavoriteForExistingPayee() {
        when(userRepository.findByDocument("52998224725")).thenReturn(Optional.of(user("Alice", "52998224725")));
        when(userRepository.findByDocument("39053344705")).thenReturn(Optional.of(user("Bob", "39053344705")));

        var result = service.add(new com.financialhub.application.port.in.ManageFavoritesUseCase.AddCommand(
                "52998224725", "52998224725", "39053344705", "Bob Santos"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).document()).isEqualTo("39053344705");
        assertThat(result.get(0).name()).isEqualTo("Bob Santos");
    }

    @Test
    void br015_cannotFavoriteSelf() {
        when(userRepository.findByDocument("52998224725")).thenReturn(Optional.of(user("Alice", "52998224725")));

        assertThatThrownBy(() -> service.add(new com.financialhub.application.port.in.ManageFavoritesUseCase.AddCommand(
                "52998224725", "52998224725", "52998224725", "Eu")))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void br015_forbiddenWhenRequesterDiffers() {
        assertThatThrownBy(() -> service.list(new com.financialhub.application.port.in.ManageFavoritesUseCase.Command(
                "52998224725", "39053344705")))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void br015_payeeMustExist() {
        when(userRepository.findByDocument("52998224725")).thenReturn(Optional.of(user("Alice", "52998224725")));
        when(userRepository.findByDocument("39053344705")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(new com.financialhub.application.port.in.ManageFavoritesUseCase.AddCommand(
                "52998224725", "52998224725", "39053344705", "Bob")))
                .isInstanceOf(UserNotFoundException.class);
    }

    private static User user(String name, String document) {
        return User.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(name.toLowerCase() + "@test.com")
                .document(document)
                .passwordHash("hash")
                .balance(new BigDecimal("1000"))
                .status(UserStatus.ACTIVE)
                .dailyLimit(new BigDecimal("5000"))
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
