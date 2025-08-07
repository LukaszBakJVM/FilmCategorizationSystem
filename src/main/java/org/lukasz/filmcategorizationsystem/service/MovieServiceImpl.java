package org.lukasz.filmcategorizationsystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.lukasz.filmcategorizationsystem.api.Language;
import org.lukasz.filmcategorizationsystem.api.Results;
import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.lukasz.filmcategorizationsystem.dto.FindMovie;
import org.lukasz.filmcategorizationsystem.dto.UpdateMovie;
import org.lukasz.filmcategorizationsystem.entity.Movie;
import org.lukasz.filmcategorizationsystem.enums.MovieSortField;
import org.lukasz.filmcategorizationsystem.exceptions.FileException;
import org.lukasz.filmcategorizationsystem.exceptions.MovieAlreadyExistsException;
import org.lukasz.filmcategorizationsystem.exceptions.MovieNotFoundException;
import org.lukasz.filmcategorizationsystem.repo.MoviesRepository;
import org.lukasz.filmcategorizationsystem.utils.CalculateRanking;
import org.lukasz.filmcategorizationsystem.utils.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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
import java.util.UUID;

@Service
public class MovieServiceImpl implements MovieService {


    private final Logger logger = LoggerFactory.getLogger(MovieServiceImpl.class);

    private final MoviesRepository repository;
    private final MoviesMapper mapper;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final CalculateRanking calculateRanking;
    private final Validation validation;

    @Value("${api.key}")
    private String apiKey;
    @Value("${movie.localFilePath}")
    private String localFilePath;


    public MovieServiceImpl(MoviesRepository repository, MoviesMapper mapper, ObjectMapper objectMapper, RestClient restClient, CalculateRanking calculateRanking, Validation validation) {

        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.calculateRanking = calculateRanking;
        this.validation = validation;
    }

    @Transactional
    @Override
    public CreateNewMovie createNewMovie(CreateNewMovie dto, MultipartFile file) {
        String uuid = UUID.randomUUID().toString();
        String fullFilePath = Paths.get(localFilePath, uuid + "-" + file.getOriginalFilename()).toString();
        validation.validation(dto);
        validation.validateVideoFile(file);
        repository.findMovieByTitle(dto.title()).ifPresent(movie -> {
            logger.error("Movie with title already exists {}", dto.title());
            throw new MovieAlreadyExistsException(String.format("Movie with title already exists: %s ", dto.title()));
        });
        long size = file.getSize();
        Movie movie = mapper.dtoToEntity(dto);
        movie.setLocalFilePath(fullFilePath);
        movie.setSizeInBytes(size);
        Language data = result(movie.getTitle());
        int ranking = calculateRanking.ranking(size, data.original_language(), data.vote_average());
        movie.setRanking(ranking);
        saveMovieOnDisc(file, uuid);

        repository.save(movie);
        return mapper.response(movie);

    }

    @SneakyThrows
    @Override
    public Resource downloadFile(String title) {
        Resource resource;

        Movie movie = repository.findMovieByTitle(title).orElseThrow(() -> new MovieNotFoundException(String.format("Movie %s not found", title)));
        String patch = movie.getLocalFilePath();
        Path filePath = Paths.get(patch);

        if (!Files.exists(filePath)) {
            logger.error("File not found on disk {}", title);
            throw new FileException("File not found on disk");


        }
        resource = new UrlResource(filePath.toUri());
        return resource;
    }

    @Override
    public List<String> sortFieldsEnums() {
        return Arrays.stream(MovieSortField.values()).map(MovieSortField::getSort).toList();
    }

    @Override
    public List<FindMovie> findAll(String param) {
        String sort = sortBy(param);
        return repository.findAll(Sort.by(sort)).stream().map(mapper::findMovie).toList();
    }

    @Transactional
    @Override
    public void updateMovie(String title, JsonMergePatch patch) {
        UpdateMovie updateMovie = updatePatch(title);
        UpdateMovie applyPatch = applyPatch(updateMovie, patch);


        Movie movie = mapper.updateMovieToEntity(applyPatch);

        repository.save(movie);
    }



    private UpdateMovie updatePatch(String findByTitle) {
        Movie movie = repository.findMovieByTitle(findByTitle).orElseThrow(() -> new MovieNotFoundException(String.format("Movie %s not found", findByTitle)));

        return mapper.toUpdateDto(movie);
    }


    private String searchMovieByTitle(final String title) {
        return UriComponentsBuilder.fromUriString("/3/search/movie").queryParam("api_key", apiKey).queryParam("query", title).build().toString();
    }


    private Language result(final String title) {

        List<Language> results = restClient.get().uri(searchMovieByTitle(title)).accept(MediaType.APPLICATION_JSON).retrieve().body(Results.class).results();
        if (!results.isEmpty()) {
            return results.getFirst();
        }
        return new Language("null", 0);
    }


    @SneakyThrows
    private UpdateMovie applyPatch(final UpdateMovie updateMovie, final JsonMergePatch patch) {
        JsonNode movieNode = objectMapper.valueToTree(updateMovie);
        JsonNode moviePatchedNode = patch.apply(movieNode);


        return objectMapper.treeToValue(moviePatchedNode, UpdateMovie.class);
    }


    private String sortBy(final String param) {
        if (param.equals("film_size")) {
            return "sizeInBytes";
        } else if (param.equals("ranking")) {
            return "ranking";
        }
        return "id";
    }


    private void saveMovieOnDisc(final MultipartFile file, String uuid) {
        Path destination = Paths.get(localFilePath, uuid + "-" + file.getOriginalFilename());

        try {

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to save file {}", file);
            throw new FileException("Failed to save file");
        }


    }


}

