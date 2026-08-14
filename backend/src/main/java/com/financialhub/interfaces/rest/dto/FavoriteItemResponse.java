package com.financialhub.interfaces.rest.dto;

import java.time.Instant;

public record FavoriteItemResponse(String document, String name, Instant savedAt) {}
