package org.lukasz.filmcategorizationsystem;

import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MoviesController {
    private final MovieServices services;

    public MoviesController(MovieServices services) {
        this.services = services;
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @ResponseStatus(HttpStatus.CREATED)
    CreateNewMovie createNewMovie(@RequestPart CreateNewMovie dto, @RequestPart MultipartFile file) {
        return services.createNewMovie(dto,file);
    }
}
