package com.financialhub.domain.model;

import java.time.Instant;

public record FavoritePayee(String document, String name, Instant savedAt) {}
