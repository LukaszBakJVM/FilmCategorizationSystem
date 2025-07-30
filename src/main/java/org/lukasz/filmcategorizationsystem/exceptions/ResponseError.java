package org.lukasz.filmcategorizationsystem.exceptions;

public record ResponseError(int status, String message) {
}
