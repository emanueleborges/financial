package com.financialhub.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record FavoriteRequest(
        @NotBlank String document,
        String name
) {}
