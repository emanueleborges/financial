package com.financialhub.interfaces.rest.dto;

import java.util.List;

public record FavoritesResponse(String document, List<FavoriteItemResponse> favorites) {}
