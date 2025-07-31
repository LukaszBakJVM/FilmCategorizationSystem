package org.lukasz.filmcategorizationsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import org.lukasz.filmcategorizationsystem.api.Language;
import org.lukasz.filmcategorizationsystem.api.Results;
import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.lukasz.filmcategorizationsystem.dto.FindMovie;
import org.lukasz.filmcategorizationsystem.enums.MovieSortField;
import org.lukasz.filmcategorizationsystem.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class MovieServices {
    private static final Set<String> SUPPORTED_VIDEO_TYPES = Set.of("video/mp4", "video/x-matroska", "video/x-msvideo");
    private final Logger logger = LoggerFactory.getLogger(MovieServices.class);
    private final LocalValidatorFactoryBean validation;
    private final MoviesRepository repository;
    private final MoviesMapper mapper;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    @Value("${api.key}")
    private String apiKey;
    @Value("${movie.localFilePath}")
    private String localFilePath;


    public MovieServices(LocalValidatorFactoryBean validation, MoviesRepository repository, MoviesMapper mapper, ObjectMapper objectMapper, RestClient restClient) {
        this.validation = validation;
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Transactional
    CreateNewMovie createNewMovie(CreateNewMovie dto, MultipartFile file) {
        String fullFilePath = Paths.get(localFilePath, file.getOriginalFilename()).toString();
        validation(dto);
        validateVideoFile(file);
        repository.findMovieByTitle(dto.title()).ifPresent(movie -> {
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


    public String searchMovieByTitle(String title) {
        return UriComponentsBuilder.fromUriString("/3/search/movie").queryParam("api_key", apiKey).queryParam("query", title).build().toString();
    }


    List<String> sortFieldsEnums() {
        return Arrays.stream(MovieSortField.values()).map(MovieSortField::getSort).toList();
    }

    List<FindMovie> findAll(String param) {
        String sort = sortBy(param);
        return repository.findAll(Sort.by(sort)).stream().map(mapper::findMovie).toList();


    }

    @Transactional
    void updateMovie(String title, JsonMergePatch patch) {
        Movie movie = repository.findMovieByTitle(title).orElseThrow();
        validation(mapper.response(movie));

        Movie movie1 = applyPatch(movie, patch);
        repository.save(movie1);
    }

    Language result(String title) {
        return restClient.get().uri(searchMovieByTitle(title)).accept(MediaType.APPLICATION_JSON).retrieve().body(Results.class).results().getFirst();
    }

    int ranking(long size, String language, double vote) {
        if (size < 209_715_200L) {
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


    private Movie applyPatch(Movie createNewMovie, JsonMergePatch patch) {
        JsonNode jobOfferNode = objectMapper.valueToTree(createNewMovie);
        JsonNode jobOfferPatchedNode;
        try {
            jobOfferPatchedNode = patch.apply(jobOfferNode);


            return objectMapper.treeToValue(jobOfferPatchedNode, Movie.class);
        } catch (JsonProcessingException | JsonPatchException e) {
            throw new InvalidPatchException("Invalid patch format or data");
        }


    }


    private String sortBy(String param) {
        if (param.equals("film_size")) {
            return "sizeInBytes";
        } else if (param.equals("ranking")) {
            return "ranking";
        }
        return "id";
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

