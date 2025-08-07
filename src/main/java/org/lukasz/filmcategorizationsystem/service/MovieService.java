package org.lukasz.filmcategorizationsystem.service;

import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.lukasz.filmcategorizationsystem.dto.FindMovie;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MovieService {
    CreateNewMovie createNewMovie(final CreateNewMovie dto, final MultipartFile file);

    Resource downloadFile(final String title);

    List<String> sortFieldsEnums();

    List<FindMovie> findAll(final String param);

    void updateMovie(final String title, final JsonMergePatch patch);

}
