package com.khomsi.grid.main.game.mapper.impl;

import com.khomsi.grid.main.game.mapper.GameMapper;
import com.khomsi.grid.main.game.model.dto.ShortGameModel;
import com.khomsi.grid.main.game.model.entity.Game;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GameMapperImpl implements GameMapper {
    @Override
    public ShortGameModel toMainGames(Game game) {
        return ShortGameModel.builder()
                .id(game.getId())
                .title(game.getTitle())
                .description(game.getDescription())
                .coverImageUrl(game.getCoverImageUrl())
                .price(game.getPrice())
                .build();
    }
}
