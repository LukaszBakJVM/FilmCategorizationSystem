package org.lukasz.filmcategorizationsystem;

import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    @GetMapping("/sortFields")
    @ResponseStatus(HttpStatus.OK)
    List<String>sortFields(){
        return services.sortFieldsEnums();
    }
    @GetMapping("/allMovies")
    @ResponseStatus(HttpStatus.OK)
    List<CreateNewMovie>allMovies(@RequestParam (defaultValue ="empty" )String sort){
        return services.findAll(sort);
    }
}
