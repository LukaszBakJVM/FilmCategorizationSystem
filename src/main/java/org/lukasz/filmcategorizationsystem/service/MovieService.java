package org.lukasz.filmcategorizationsystem.service;

import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.lukasz.filmcategorizationsystem.dto.FindMovie;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

public interface MovieService {
    CreateNewMovie createNewMovie( CreateNewMovie dto,  MultipartFile file);

    Resource downloadFile( String title);

    List<String> sortFieldsEnums();

    List<FindMovie> findAll(String param);

    void updateMovie( String title,  JsonMergePatch patch);

}
