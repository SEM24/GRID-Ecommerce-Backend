package com.khomsi.grid.additional.genre.service;

import com.khomsi.grid.additional.genre.GenreRepository;
import com.khomsi.grid.additional.genre.model.entity.Genre;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class GenreServiceImpl implements GenreService {
    private final GenreRepository genreRepository;

    @Override
    public List<Genre> getAllGenres() {
        return genreRepository.findAll();
    }
}
