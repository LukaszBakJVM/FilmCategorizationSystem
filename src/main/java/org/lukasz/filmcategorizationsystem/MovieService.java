package org.lukasz.filmcategorizationsystem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import lombok.SneakyThrows;
import org.lukasz.filmcategorizationsystem.api.Language;
import org.lukasz.filmcategorizationsystem.api.Results;
import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.lukasz.filmcategorizationsystem.dto.FindMovie;
import org.lukasz.filmcategorizationsystem.enums.MovieSortField;
import org.lukasz.filmcategorizationsystem.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

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
public class MovieService {


    private final Logger logger = LoggerFactory.getLogger(MovieService.class);
    private final LocalValidatorFactoryBean validation;
    private final MoviesRepository repository;
    private final MoviesMapper mapper;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${api.key}")
    private String apiKey;
    @Value("${movie.localFilePath}")
    private String localFilePath;


    public MovieService(LocalValidatorFactoryBean validation, MoviesRepository repository, MoviesMapper mapper, ObjectMapper objectMapper, RestClient restClient) {
        this.validation = validation;
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.restClient = restClient;

    }

    @Transactional
    CreateNewMovie createNewMovie(final CreateNewMovie dto, final MultipartFile file) {
        String fullFilePath = Paths.get(localFilePath, file.getOriginalFilename()).toString();
        validation(dto);
        validateVideoFile(file);
        repository.findMovieByTitle(dto.title()).ifPresent(movie -> {
            logger.error("Movie with title already exists {}",dto.title());
            throw new MovieAlreadyExistsException(String.format("Movie with title already exists: %s ", dto.title()));
        });
        long size = file.getSize();
        Movie movie = mapper.dtoToEntity(dto);
        movie.setLocalFilePath(fullFilePath);
        movie.setSizeInBytes(size);
        Language data = result(movie.getTitle());
        int ranking = ranking(size, data.original_language(), data.vote_average());
        movie.setRanking(ranking);
        saveMovieOnDisc(file);

        repository.save(movie);
        return mapper.response(movie);

    }

    @SneakyThrows
    Resource downloadFile(final String title) {
        Resource resource;

        Movie movie = repository.findMovieByTitle(title).orElseThrow(() -> new MovieNotFoundException(String.format("Movie %s not found", title)));
        String patch = movie.getLocalFilePath();
        Path filePath = Paths.get(patch);

        if (!Files.exists(filePath)) {
            logger.error("File not found on disk {}",title);
            throw new FileException("File not found on disk");


        }
        resource = new UrlResource(filePath.toUri());
        return resource;

    }

    private String searchMovieByTitle(final String title) {
        return UriComponentsBuilder.fromUriString("/3/search/movie").queryParam("api_key", apiKey).queryParam("query", title).build().toString();
    }


    List<String> sortFieldsEnums() {
        return Arrays.stream(MovieSortField.values()).map(MovieSortField::getSort).toList();
    }

    List<FindMovie> findAll(final String param) {
        String sort = sortBy(param);
        return repository.findAll(Sort.by(sort)).stream().map(mapper::findMovie).toList();


    }

    @Transactional
    void updateMovie(final String title, final JsonMergePatch patch) {
        Movie movie = repository.findMovieByTitle(title).orElseThrow(() -> new MovieNotFoundException(String.format("Movie %s not found", title)));

        Movie applyPatch = applyPatch(movie, patch);
        repository.save(applyPatch);
    }

    private Language result(final String title) {

        List<Language> results = restClient.get().uri(searchMovieByTitle(title)).accept(MediaType.APPLICATION_JSON).retrieve().body(Results.class).results();
        if (!results.isEmpty()) {
            return results.getFirst();
        }
        return new Language("null", 0);
    }

    private int ranking(final long size, final String language, final double vote) {
        long smallFile = 209_715_200L;
        if (size < smallFile) {
            return 100;
        }
        int ranking = 0;

        if ("pl".equalsIgnoreCase(language)) {
            ranking += 200;

        }

        if (vote >= 5.0) {
            ranking += 100;
        }


        return ranking;
    }


    @SneakyThrows
    private Movie applyPatch(final Movie createNewMovie, final JsonMergePatch patch) {
        JsonNode movieNode = objectMapper.valueToTree(createNewMovie);
        JsonNode moviePatchedNode = patch.apply(movieNode);


        return objectMapper.treeToValue(moviePatchedNode, Movie.class);
    }


    private String sortBy(final String param) {
        if (param.equals("film_size")) {
            return "sizeInBytes";
        } else if (param.equals("ranking")) {
            return "ranking";
        }
        return "id";
    }


    private void saveMovieOnDisc(final MultipartFile file) {
        Path destination = Paths.get(localFilePath, file.getOriginalFilename());

        try {

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to save file {}",file);
            throw new FileException("Failed to save file");
        }


    }


    private <T> void validation(T t) {

        Set<ConstraintViolation<T>> violations = validation.validate(t);


        if (!violations.isEmpty()) {
            String errorMessage = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(" , "));

            logger.error("inside validation  errors: {}", errorMessage);

            throw new CustomValidationException(errorMessage);
        }


    }

    private void validateVideoFile(MultipartFile file) {
        Set<String> supportedVideoTypes = Set.of("video/mp4", "video/x-matroska", "video/x-msvideo");

        String contentType = file.getContentType();

        if (!supportedVideoTypes.contains(contentType)) {
            throw new MediaFileException("Only video files (.mp4, .avi, .mkv) are allowed");
        }
    }
}

