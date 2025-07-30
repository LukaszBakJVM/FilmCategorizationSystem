package org.lukasz.filmcategorizationsystem.enums;

public enum MovieSortField {


    RANKING("ranking"),
    FILM_SIZE("film_size");

    private final String sort;

    MovieSortField(String sort) {
        this.sort = sort;
    }

    public String getSort() {
        return sort;
    }
}
