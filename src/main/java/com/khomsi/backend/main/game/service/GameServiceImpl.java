package com.khomsi.backend.main.game.service;

import com.khomsi.backend.additional.genre.model.entity.Genre;
import com.khomsi.backend.main.game.GameRepository;
import com.khomsi.backend.main.game.mapper.GameMapper;
import com.khomsi.backend.main.game.model.dto.*;
import com.khomsi.backend.main.game.model.entity.Game;
import com.khomsi.backend.main.handler.exception.GlobalServiceException;
import com.khomsi.backend.main.user.service.UserInfoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static com.khomsi.backend.main.utils.Utils.createSorting;

@Service
@Slf4j
@AllArgsConstructor
public class GameServiceImpl implements GameService {
    private final GameRepository gameRepository;
    private final GameMapper gameMapper;
    private final UserInfoService userInfoService;

    //TODO Write integration tests with cucumber for this endpoint
    @Override
    public GeneralGame getExtendedGamesByPage(GameCriteria gameCriteria, boolean applyActiveFilter) {
        int page = gameCriteria.getPage();

        Sort sorting = createSorting(gameCriteria.getSort(), "id");
        Pageable pagingSort = PageRequest.of(page, gameCriteria.getSize(), sorting);
        Specification<Game> specification = Specification.where(null);
        String transformedWord = (gameCriteria.getTitle() != null) ? transformWord(gameCriteria.getTitle()) : "";
        specification = specification.and(GameSpecifications.byIdList(gameCriteria.getId()));
        specification = specification.and(GameSpecifications.byTitle(transformedWord));
        specification = specification.and(GameSpecifications.byMaxPrice(gameCriteria.getMaxPrice()));
        specification = specification.and(GameSpecifications.byTagIds(gameCriteria.getTags()));
        specification = specification.and(GameSpecifications.byField("genres",
                "name", gameCriteria.getGenres()));
        specification = specification.and(GameSpecifications.byField("platforms",
                "name", gameCriteria.getPlatforms()));
        specification = specification.and(GameSpecifications.byField("developer",
                "name", gameCriteria.getDevelopers()));
        specification = specification.and(GameSpecifications.byField("publisher",
                "name", gameCriteria.getPublishers()));
        // Check if the active filter should be applied
        if (applyActiveFilter) {
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.isTrue(root.get("active")));
        }

        Page<Game> gamePage = gameRepository.findAll(specification, pagingSort);
        if (gamePage.isEmpty()) {
            throw new GlobalServiceException(HttpStatus.NOT_FOUND, "Games are not found in the database.");
        }
        BigDecimal maxPrice = gameRepository.findMaxPrice();

        List<ShortGameModel> shortGameModels = gamePage
                .map(game -> {
                    boolean ownedByCurrentUser = userInfoService.checkIfGameIsOwnedByCurrentUser(game);
                    return gameMapper.toShortGame(game, ownedByCurrentUser);
                }).getContent();

        return GeneralGame.builder()
                .games(shortGameModels)
                .totalItems(gamePage.getTotalElements())
                .totalPages(gamePage.getTotalPages() - 1)
                .maxPrice(maxPrice)
                .currentPage(page)
                .build();
    }

    @Override
    public List<GameModelWithGenreLimit> getGamesByGenre(int qty, String excludedGenre) {
        return gameRepository.findGamesByGenre(excludedGenre).stream()
                .filter(game -> {
                    Set<Genre> genres = game.getGenres();
                    if (genres.size() > 2) {
                        genres.removeIf(genre -> genre.getName().equals(excludedGenre));
                    }
                    return genres.size() <= 2;
                })
                .limit(qty)
                .map(game -> {
                    boolean ownedByCurrentUser = userInfoService.checkIfGameIsOwnedByCurrentUser(game);
                    return gameMapper.toLimitGenreGame(game, ownedByCurrentUser);
                }).toList();
    }

    @Override
    public List<PopularGameModel> getPopularQtyOfGames(int gameQuantity) {
        List<PopularGameModel> shortGameModels = gameRepository.findAll().stream()
                .map(game -> {
                    boolean ownedByCurrentUser = userInfoService.checkIfGameIsOwnedByCurrentUser(game);
                    return gameMapper.toPopularGame(game, ownedByCurrentUser);
                }).toList();
        return getRandomGames(shortGameModels, gameQuantity);
    }

    @Override
    public List<GameModelWithGenreLimit> getRandomQtyOfGames(int gameQuantity) {
        List<GameModelWithGenreLimit> shortGameModels = gameRepository.findAll().stream()
                .map(game -> {
                    boolean ownedByCurrentUser = userInfoService.checkIfGameIsOwnedByCurrentUser(game);
                    return gameMapper.toLimitGenreGame(game, ownedByCurrentUser);
                }).toList();
        return getRandomGames(shortGameModels, gameQuantity);
    }

    @Override
    public Game getActiveGameById(Long gameId) {
        return gameRepository.findByIdAndActiveTrue(gameId).orElseThrow(() ->
                new GlobalServiceException(HttpStatus.NOT_FOUND, "Game with id " + gameId + " is not found."));
    }

    @Override
    public Game getGameById(Long gameId) {
        return gameRepository.findById(gameId).orElseThrow(() ->
                new GlobalServiceException(HttpStatus.NOT_FOUND, "Game with id " + gameId + " is not found."));
    }

    @Override
    public ExtendedGame getExtendedGameById(Long gameId) {
        Game game = getActiveGameById(gameId);
        return new ExtendedGame(getActiveGameById(gameId), userInfoService.checkIfGameIsOwnedByCurrentUser(game));
    }

    @Override
    public List<PopularGameModel> getSpecialOffers(String query, int qty) {
        //TODO refactor the method in future
        List<Game> games = switch (query) {
            case "release date" -> gameRepository.findGamesByEarliestReleaseDate();
            case "discount" -> gameRepository.findGamesWithDiscount();
            //TODO no metrics yet to use it not as a random
            case "sales" -> getRandomGames(gameRepository.findAll(), qty);
            default -> throw new GlobalServiceException(HttpStatus.NOT_FOUND, "Games are not found in database.");
        };
        return games.stream()
                .map(game -> {
                    boolean ownedByCurrentUser = userInfoService.checkIfGameIsOwnedByCurrentUser(game);
                    return gameMapper.toPopularGame(game, ownedByCurrentUser);
                })
                .limit(qty)
                .toList();
    }

    @Override
    public List<GameModelWithGenreLimit> searchGamesByTitle(String text, int qty) {
        return gameRepository.findSimilarTitles(transformWord(text)).stream()
                .map(game -> {
                    boolean ownedByCurrentUser = userInfoService.checkIfGameIsOwnedByCurrentUser(game);
                    return gameMapper.toLimitGenreGame(game, ownedByCurrentUser);
                })
                .limit(qty)
                .toList();
    }

    @Override
    public String transformWord(String word) {
        return word.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining("%", "", "%"));
    }

    private <T> List<T> getRandomGames(List<T> gameModels, int gameQuantity) {
        return (gameModels.size() <= gameQuantity) ? gameModels :
                new Random().ints(0, gameModels.size())
                        .distinct()
                        .limit(gameQuantity)
                        .mapToObj(gameModels::get).toList();
    }
}