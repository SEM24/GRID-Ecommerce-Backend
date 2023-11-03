package com.khomsi.grid.main.game.model.dto;

import lombok.Builder;

import java.util.List;


@Builder
public record GeneralGame(List<MainGameModel> games, long totalItems, int totalPages, int currentPage) {
}

