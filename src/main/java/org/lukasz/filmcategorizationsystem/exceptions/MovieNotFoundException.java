package org.lukasz.filmcategorizationsystem.exceptions;

public class MovieNotFoundException extends RuntimeException{
    public MovieNotFoundException(String message) {
        super(message);
    }
}
