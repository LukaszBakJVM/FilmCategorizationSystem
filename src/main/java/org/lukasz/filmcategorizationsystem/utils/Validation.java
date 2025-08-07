package org.lukasz.filmcategorizationsystem.utils;

import org.springframework.web.multipart.MultipartFile;

public interface Validation {
    <T> void validation(T t);
  void   validateVideoFile(MultipartFile file);
}
