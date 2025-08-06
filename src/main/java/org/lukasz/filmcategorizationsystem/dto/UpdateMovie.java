package org.lukasz.filmcategorizationsystem.dto;

public record UpdateMovie(Long id, String title, String director,  int productionYear, int ranking, long sizeInBytes, String localFilePath) {
}
