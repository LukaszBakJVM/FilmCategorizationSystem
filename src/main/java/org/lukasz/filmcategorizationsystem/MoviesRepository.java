package org.lukasz.filmcategorizationsystem;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MoviesRepository extends JpaRepository<Movie, Long> {
    Optional<Movie>findMovieByTitle(String title);


}