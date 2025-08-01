package org.lukasz.filmcategorizationsystem;

import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.lukasz.filmcategorizationsystem.dto.FindMovie;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/movies")
public class MoviesController {
    private final MovieServices services;


    public MoviesController(MovieServices services) {
        this.services = services;

    }

    @PostMapping( value = "/addMovie",consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    CreateNewMovie createNewMovie(@RequestPart final CreateNewMovie dto, @RequestPart final MultipartFile file) {
        return services.createNewMovie(dto, file);
    }

    @GetMapping("/sortFields")
    @ResponseStatus(HttpStatus.OK)
    List<String> sortFields() {
        return services.sortFieldsEnums();
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    List<FindMovie> allMovies(@RequestParam(required = false, defaultValue = "id") final String sort) {
        return services.findAll(sort);
    }

    @PatchMapping("update/{title}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateMovie(@PathVariable final String title, @RequestBody final JsonMergePatch patch) {
        services.updateMovie(title, patch);

    }
    @GetMapping("/download/{title}")
    ResponseEntity<Resource> downloadFile (@PathVariable final String title){
        Resource file = services.downloadFile(title);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename())
                .body(file);

    }


}
