package org.lukasz.filmcategorizationsystem;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.lukasz.filmcategorizationsystem.dto.FindMovie;
import org.lukasz.filmcategorizationsystem.exceptions.*;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Rollback
@AutoConfigureWebTestClient
class FilmCategorizationSystemApplicationTests {


    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest").withInitScript("data.sql");
    @LocalServerPort
    private static int dynamicPort;
    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance().options(wireMockConfig().port(dynamicPort)).build();
    @Autowired
    MoviesRepository moviesRepository;
    @Autowired
    Gson gson;
    Response response = new Response();
    @Autowired
    ExceptionsController exceptionsController;
    @Autowired
    WebTestClient webTestClient;


    @Autowired
    private MoviesController moviesController;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("baseUrl", wireMockServer::baseUrl);
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + postgreSQLContainer.getHost() + ":" + postgreSQLContainer.getFirstMappedPort() + "/" + postgreSQLContainer.getDatabaseName());
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

    }

    @BeforeAll
    static void startPostgres() {
        postgreSQLContainer.start();


    }

    @AfterAll
    static void stopPostgres() {
        postgreSQLContainer.stop();
    }


    @Test
    void shouldAddMovieWithMultipartDataUnder200MB() {
        CreateNewMovie dto = new CreateNewMovie("terminator", "director", 2011);
        int sizeInBytes = 100 * 1024 * 1024; //100MB


        byte[] content = new byte[sizeInBytes];


        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.mp4", "video/mp4", content);

        moviesController.createNewMovie(dto, mockFile);
        Movie movie = moviesRepository.findMovieByTitle("terminator").orElseThrow();

        assertEquals("director", movie.getDirector());
        assertEquals(2011, movie.getProductionYear());
        assertEquals(100, movie.getRanking());
        assertEquals(104857600, movie.getSizeInBytes());

    }

    @Test
    void shouldAddMovieWithMultipartDataOver200MBAndPolish() {
        CreateNewMovie dto = new CreateNewMovie("Boże Ciało", "director1", 2011);

        int sizeInBytes = 250 * 1024 * 1024;  //250MB
        //Language[original_language=pl, vote_average=7.6]


        byte[] content = new byte[sizeInBytes];


        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.mp4", "video/mp4", content);

        moviesController.createNewMovie(dto, mockFile);
        Movie movie = moviesRepository.findMovieByTitle("Boże Ciało").orElseThrow();

        assertEquals("director1", movie.getDirector());
        assertEquals(2011, movie.getProductionYear());
        assertEquals(300, movie.getRanking());
        assertEquals(262144000, movie.getSizeInBytes());

    }

    @Test
    void AddMovieWithMultipartDataUnder200MBWhenTitleExist() {
        CreateNewMovie dto = new CreateNewMovie("title1", "director", 2011);
        int sizeInBytes = 100 * 1024 * 1024; //100MB


        byte[] content = new byte[sizeInBytes];

        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.mp4", "video/mp4", content);

        assertThrows(MovieAlreadyExistsException.class, () -> moviesController.createNewMovie(dto, mockFile));

    }

    @Test
    void shouldThrowCustomValidationException_forInvalidUserDat() {
        CreateNewMovie dto = new CreateNewMovie("", "director", 2011);
        int sizeInBytes = 100 * 1024 * 1024; //100MB


        byte[] content = new byte[sizeInBytes];

        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.mp4", "video/mp4", content);

        assertThrows(CustomValidationException.class, () -> moviesController.createNewMovie(dto, mockFile));

    }

    @Test
    void shouldThrowCustomValidationException_forMovieTitleOver300Characters() {
        String s = "aaa";

        String repeat = s.repeat(101);

        CreateNewMovie dto = new CreateNewMovie(repeat ,"director", 2011);
        int sizeInBytes = 100 * 1024 * 1024; //100MB


        byte[] content = new byte[sizeInBytes];

        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.mp4", "video/mp4", content);

        assertThrows(CustomValidationException.class, () -> moviesController.createNewMovie(dto, mockFile));

    }

    @Test
    void shouldThrowMediaFileException_forInvalidMovieFormat() {
        CreateNewMovie dto = new CreateNewMovie("titlemp4", "director", 2011);
        int sizeInBytes = 100 * 1024 * 1024; //100MB


        byte[] content = new byte[sizeInBytes];

        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.txt", "txt", content);

        assertThrows(MediaFileException.class, () -> moviesController.createNewMovie(dto, mockFile));

    }

    @Test
    void shouldReturnRanking_0_when_MovieNotFountOnApi() {
        CreateNewMovie dto = new CreateNewMovie("notfoundfilm", "director", 2011);

        int sizeInBytes = 250 * 1024 * 1024;  //250MB


        byte[] content = new byte[sizeInBytes];

        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.mp4", "video/mp4", content);

        moviesController.createNewMovie(dto, mockFile);


        Movie movie = moviesRepository.findMovieByTitle("notfoundfilm").orElseThrow();

        assertEquals("director", movie.getDirector());
        assertEquals(2011, movie.getProductionYear());
        assertEquals(0, movie.getRanking());
        assertEquals(262144000, movie.getSizeInBytes());


    }


    @Test
    void shouldAShowAllFilmsSortByRanking() {
        List<FindMovie> ranking = moviesController.allMovies("ranking");
        String json = gson.toJson(ranking);

        JSONAssert.assertEquals(json, response.ranking, true);


    }

    @Test
    void shouldAShowAllFilmsSortBySize() {

        List<FindMovie> filmSize = moviesController.allMovies("film_size");
        String json = gson.toJson(filmSize);

        JSONAssert.assertEquals(json, response.film_size, true);


    }

    @Test
    void shouldAShowAllFilmsSortById() {
        List<FindMovie> ranking = moviesController.allMovies("id");
        String json = gson.toJson(ranking);

        JSONAssert.assertEquals(json, response.id, true);

    }

    @Test
    void shouldAShowAllFilmsSortByIdWhenSortNotPresent() {

        webTestClient.get().uri("/movies/all").accept(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isOk().expectBody().json(response.id, JsonCompareMode.STRICT);

    }


    @Test
    void shouldAShowAllSortFields() {
        List<String> sortFields = moviesController.sortFields();

        String json = gson.toJson(sortFields);

        JSONAssert.assertEquals(json, response.sortFields, true);


    }

    @Test
    void shouldApplyPatchSuccessfully() throws JsonPatchException, IOException {


        String patchStr = "{ \"director\": \"New director\" }";

        JsonNode patchNode = JsonLoader.fromString(patchStr);
        JsonMergePatch patch = JsonMergePatch.fromJson(patchNode);


        moviesController.updateMovie("title1", patch);

        Movie movie = moviesRepository.findMovieByTitle("title1").orElseThrow();
        assertEquals("New director", movie.getDirector());
        assertNotEquals("director1", movie.getDirector());

    }


    @Test
    void testValidationException_whenTitleBlank() {
        CustomValidationException ex = new CustomValidationException("Title cannot be blank");
        ResponseError error = exceptionsController.movieValidationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.status());
        assertEquals("Title cannot be blank", error.message());
    }

    @Test
    void testUnsupportedMediaFile() {
        MediaFileException ex = new MediaFileException("Unsupported media type");
        ResponseError error = exceptionsController.unsupportedMediaFile(ex);
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), error.status());
        assertEquals("Unsupported media type", error.message());
    }

    @Test
    void testFileException() {
        FileException ex = new FileException("Failed to save file");
        ResponseError error = exceptionsController.saveFileException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), error.status());
        assertEquals("Failed to save file", error.message());


    }

    @Test
    void testMovieAlreadyExistsException() {
        MovieAlreadyExistsException ex = new MovieAlreadyExistsException("Movie with title already exists: test1");
        ResponseError error = exceptionsController.movieExist(ex);
        assertEquals(HttpStatus.CONFLICT.value(), error.status());
        assertEquals("Movie with title already exists: test1", error.message());


    }

    @Test
    void shouldThrowFileException_whenFileNotFound() {

        String title = "title3";

        assertThrows(FileException.class, () -> moviesController.downloadFile(title));


    }

    @Test
    void shouldReturnFileAsResource() {
        String title = "title2";

        ResponseEntity<Resource> resource = moviesController.downloadFile(title);

        assertEquals(200, resource.getStatusCode().value());


    }


}
