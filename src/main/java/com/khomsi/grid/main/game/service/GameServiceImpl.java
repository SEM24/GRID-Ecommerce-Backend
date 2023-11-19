package com.khomsi.grid.main.game.service;

import com.khomsi.grid.additional.genre.GenreRepository;
import com.khomsi.grid.additional.genre.model.entity.Genre;
import com.khomsi.grid.main.game.GameRepository;
import com.khomsi.grid.main.game.handler.exception.GameNotFoundException;
import com.khomsi.grid.main.game.mapper.GameMapper;
import com.khomsi.grid.main.game.model.dto.GameModelWithLimit;
import com.khomsi.grid.main.game.model.dto.GeneralGame;
import com.khomsi.grid.main.game.model.dto.PopularGameModel;
import com.khomsi.grid.main.game.model.dto.ShortGameModel;
import com.khomsi.grid.main.game.model.entity.Game;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
@Slf4j
@AllArgsConstructor
public class GameServiceImpl implements GameService {
    private final GameRepository gameRepository;
    private final GameMapper gameMapper;
    private final GenreRepository genreRepository;

    @Override
    public GeneralGame getGamesByPage(int page, int pageSize, String[] sort, String title) {
        Sort sorting = createSorting(sort);
        Pageable pagingSort = PageRequest.of(page, pageSize, sorting);
        Page<Game> gamePage = title == null
                ? gameRepository.findAll(pagingSort)
                : gameRepository.findGameByTitleContainingIgnoreCase(title, pagingSort);

        if (gamePage.isEmpty()) {
            throw new GameNotFoundException();
        }
        List<ShortGameModel> shortGameModels = gamePage
                .map(gameMapper::toShortGame)
                .getContent();

        return GeneralGame.builder()
                .games(shortGameModels)
                .totalItems(gamePage.getTotalElements())
                .totalPages(gamePage.getTotalPages())
                .currentPage(page)
                .build();
    }

    public List<GameModelWithLimit> getGamesByGenre(int qty, String excludedGenre) {
        return gameRepository.findGamesByGenre(excludedGenre).stream()
                .filter(game -> {
                    Set<Genre> genres = game.getGenres();
                    if (genres.size() > 2) {
                        genres.removeIf(genre -> genre.getName().equals(excludedGenre));
                    }
                    return genres.size() <= 2;
                })
                .limit(qty)
                .map(gameMapper::toLimitGame)
                .toList();
    }

    @Override
    public List<PopularGameModel> getPopularQtyOfGames(int gameQuantity) {
        List<PopularGameModel> shortGameModels = gameRepository.findAll().stream()
                .map(gameMapper::toPopularGame)
                .toList();
        return getRandomGames(shortGameModels, gameQuantity);
    }

    @Override
    public List<GameModelWithLimit> getRandomQtyOfGames(int gameQuantity) {
        List<GameModelWithLimit> shortGameModels = gameRepository.findAll().stream()
                .map(gameMapper::toLimitGame)
                .toList();
        return getRandomGames(shortGameModels, gameQuantity);
    }

    @Override
    public Game getGameById(Long gameId) {
        return gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
    }

    private <T> List<T> getRandomGames(List<T> gameModels, int gameQuantity) {
        return (gameModels.size() <= gameQuantity) ? gameModels :
                new Random().ints(0, gameModels.size())
                        .distinct()
                        .limit(gameQuantity)
                        .mapToObj(gameModels::get).toList();
    }

    private Sort createSorting(String[] sort) {
        return (sort != null && sort.length == 2) ?
                Sort.by(sort[1].equalsIgnoreCase("asc") ? ASC : DESC, sort[0]) :
                Sort.by(DESC, "id");
    }
}