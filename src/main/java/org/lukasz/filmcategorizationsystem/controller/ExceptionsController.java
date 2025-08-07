package org.lukasz.filmcategorizationsystem.controller;

import org.lukasz.filmcategorizationsystem.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionsController {


    @ExceptionHandler(CustomValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseError movieValidationException(CustomValidationException ex) {
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(MediaFileException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseError unsupportedMediaFile(MediaFileException ex) {
        return new ResponseError(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ex.getMessage());
    }

    @ExceptionHandler(FileException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseError saveFileException(FileException ex) {
        return new ResponseError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
    }

    @ExceptionHandler(MovieAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseError movieExist(MovieAlreadyExistsException ex) {
        return new ResponseError(HttpStatus.CONFLICT.value(), ex.getMessage());
    }
    @ExceptionHandler(MovieNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseError movieNotFound(MovieNotFoundException ex) {
        return new ResponseError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }


}
