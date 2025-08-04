package org.lukasz.filmcategorizationsystem;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lukasz.filmcategorizationsystem.dto.CreateNewMovie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/clear-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class WebTestClientTest {

    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest");
    @LocalServerPort
    private static int dynamicPort;
    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance().options(wireMockConfig().port(dynamicPort)).build();
    @Autowired
    MoviesRepository moviesRepository;
    @Autowired
    WebTestClient webTestClient;

    Response response = new Response();


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
    void shouldApplyPatchSuccessfully() {


        String patchJson = "{ \"director\": \"New director\" }";

        webTestClient.patch().uri("/movies/update/title1").contentType(MediaType.valueOf("application/merge-patch+json")).bodyValue(patchJson).exchange().expectStatus().isNoContent();


        Movie movie = moviesRepository.findMovieByTitle("title1").orElseThrow();
        assertEquals("New director", movie.getDirector());
        assertNotEquals("director1", movie.getDirector());
    }

    @Test
    void shouldAShowAllFilmsSortByRanking() {

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/movies/all").queryParam("sort", "ranking").build()).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectBody().json(response.ranking, JsonCompareMode.STRICT);


    }

    @Test
    void shouldAShowAllFilmsSortBySize() {
        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/movies/all").queryParam("sort", "film_size").build()).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectBody().json(response.film_size, JsonCompareMode.STRICT);

    }


    @Test
    void shouldAShowAllFilmsSortByIdWhenSortNotPresent() {

        webTestClient.get().uri("/movies/all").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectBody().json(response.id, JsonCompareMode.STRICT);

    }


    @Test
    void addMovieWithMultipartDataUnder200MBWhenTitleExist() {

        String responseError = """
                {"status":409,"message":"Movie with title already exists: title1 "}
                """;

        CreateNewMovie dto = new CreateNewMovie("title1", "director1", 2011);
        int sizeInBytes = 1024 * 1024;
        byte[] content = new byte[sizeInBytes];

        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.mp4", "video/mp4", content);


        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("dto", dto).header("Content-Type", "application/json");
        builder.part("file", mockFile.getResource()).header("Content-Type", "video/mp4");


        webTestClient.post().uri("/movies/addMovie").body(BodyInserters.fromMultipartData(builder.build())).exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT).expectBody().json(responseError);

    }

    @Test
    void shouldThrowCustomValidationException_forInvalidUserDat() {
        String responseError = """
                {"status":400,"message":"Title cannot be blank"}
                """;

        CreateNewMovie dto = new CreateNewMovie("", "director", 2011);
        int sizeInBytes = 1024 * 1024;


        byte[] content = new byte[sizeInBytes];

        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.mp4", "video/mp4", content);


        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("dto", dto).header("Content-Type", "application/json");
        builder.part("file", mockFile.getResource()).header("Content-Type", "video/mp4");


        webTestClient.post().uri("/movies/addMovie").body(BodyInserters.fromMultipartData(builder.build())).exchange().expectStatus().isBadRequest().expectBody().json(responseError);
    }
}
