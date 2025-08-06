package org.lukasz.filmcategorizationsystem;

import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.lukasz.filmcategorizationsystem.dto.FindMovie;
import org.lukasz.filmcategorizationsystem.dto.UpdateMovie;
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
    FindMovie findMovie(Movie movie){
        return new FindMovie(movie.getTitle(), movie.getDirector(), movie.getProductionYear(), movie.getRanking(), movie.getSizeInBytes());
    }
    UpdateMovie toUpdateDto(Movie movie){
        return new UpdateMovie(movie.getId(), movie.getTitle(), movie.getDirector(),
                movie.getProductionYear(), movie.getRanking(),movie.getSizeInBytes(),movie.getLocalFilePath());
    }
    Movie updateMovieToEntity(UpdateMovie updateMovie){
        Movie movie = new Movie();
        movie.setId(updateMovie.id());
        movie.setTitle(updateMovie.title());
        movie.setDirector(updateMovie.director());
        movie.setProductionYear(updateMovie.productionYear());
        movie.setRanking(updateMovie.ranking());
        movie.setSizeInBytes(updateMovie.sizeInBytes());
        movie.setLocalFilePath(updateMovie.localFilePath());
        return movie;
    }
}
