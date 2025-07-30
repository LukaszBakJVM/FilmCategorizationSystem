package org.lukasz.filmcategorizationsystem;

import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.springframework.stereotype.Component;

@Component
public class MoviesMapper {
    Movie dtoToEntity(CreateNewMovie createNewMovie){
        Movie movie = new Movie();
        movie.setTitle(createNewMovie.title());
        movie.setDirector(createNewMovie.director());
        movie.setProductionYear(createNewMovie.productionYear());
        return movie;
    }
    CreateNewMovie response(Movie movie){
        return new CreateNewMovie(movie.getTitle(), movie.getDirector(), movie.getProductionYear());
    }
}
