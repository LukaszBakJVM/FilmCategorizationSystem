package org.lukasz.filmcategorizationsystem.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionsController {


    @ExceptionHandler(CustomValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseError userNotFoundException(CustomValidationException ex) {
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }
    @ExceptionHandler(MediaFileException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseError unsupportedMediaFile(MediaFileException ex){
        return new ResponseError(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ex.getMessage());
    }
    @ExceptionHandler(SaveFileException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseError saveFileException(SaveFileException ex){
        return new ResponseError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
    }


}
