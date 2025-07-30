package org.lukasz.filmcategorizationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNewMovie(
        @NotBlank(message = "Title cannot be blank") @Size(max = 300, message = "Title must be at most 300 characters") String title,
        @NotBlank(message = "Director cannot be blank") @Size(max = 200, message = "Director must be at most 200 characters") String director,
        int productionYear) {
}
