package org.lukasz.filmcategorizationsystem.exceptions;

public class MovieAlreadyExistsException extends RuntimeException{
    public MovieAlreadyExistsException(String message) {
        super(message);
    }
}
