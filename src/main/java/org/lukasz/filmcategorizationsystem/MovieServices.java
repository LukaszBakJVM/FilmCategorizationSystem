package org.lukasz.filmcategorizationsystem;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.lukasz.filmcategorizationsystem.enums.MovieSortField;
import org.lukasz.filmcategorizationsystem.exceptions.CustomValidationException;
import org.lukasz.filmcategorizationsystem.exceptions.MediaFileException;
import org.lukasz.filmcategorizationsystem.exceptions.MovieAlreadyExistsException;
import org.lukasz.filmcategorizationsystem.exceptions.SaveFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MovieServices {
    private static final Set<String> SUPPORTED_VIDEO_TYPES = Set.of("video/mp4", "video/x-matroska", "video/x-msvideo");
    private final Logger logger = LoggerFactory.getLogger(MovieServices.class);
    private final LocalValidatorFactoryBean validation;
    private final MoviesRepository repository;
    private final MoviesMapper mapper;
    @Value("${movie.localFilePath}")
    private String localFilePath;


    public MovieServices(LocalValidatorFactoryBean validation, MoviesRepository repository, MoviesMapper mapper) {
        this.validation = validation;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    CreateNewMovie createNewMovie(CreateNewMovie dto, MultipartFile file) {
        String fullFilePath = localFilePath + "/" + file.getOriginalFilename();
        validation(dto);
        validateVideoFile(file);
        repository.findMovieByTitle(dto.title()).ifPresent(movie -> {
            throw new MovieAlreadyExistsException("Movie with title already exists: " + dto.title());
        });
        Movie movie = mapper.dtoToEntity(dto);
        movie.setLocalFilePath(fullFilePath);
        movie.setSizeInBytes(file.getSize());
        saveMovieOnDisc(file);
        repository.save(movie);
        return mapper.response(movie);

    }

    List<String> sortFieldsEnums() {
        return Arrays.stream(MovieSortField.values()).map(MovieSortField::getSort).toList();
    }

    List<CreateNewMovie> findAll(String param) {
        if (param.equals("film_size")) {
            return findAllAndSortBySize();
        } else if (param.equals("ranking")) {
            return findAllAndSortByRanking();
        }
        return repository.findAll().stream().map(mapper::response).toList();


    }

    private List<CreateNewMovie> findAllAndSortBySize() {
        return repository.findAll(Sort.by("sizeInBytes")).stream().map(mapper::response).toList();
    }

    private List<CreateNewMovie> findAllAndSortByRanking() {
        return repository.findAll(Sort.by("ranking")).stream().map(mapper::response).toList();
    }

    private void saveMovieOnDisc(MultipartFile file) {
        Path destination = Paths.get(localFilePath, file.getOriginalFilename());

        try {

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new SaveFileException("Failed to save file");
        }


    }


    private <T> void validation(T t) {

        Set<ConstraintViolation<T>> violations = validation.validate(t);


        if (!violations.isEmpty()) {
            String errorMessage = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(" , "));

            logger.info("inside validation  errors: {}", errorMessage);

            throw new CustomValidationException(errorMessage);
        }


    }

    private void validateVideoFile(MultipartFile file) {
        String contentType = file.getContentType();

        if (!SUPPORTED_VIDEO_TYPES.contains(contentType)) {
            throw new MediaFileException("Only video files (.mp4, .avi, .mkv) are allowed");
        }
    }
}

