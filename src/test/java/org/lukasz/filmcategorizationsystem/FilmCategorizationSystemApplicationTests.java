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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    WebTestClient webTestClient;
    Response response = new Response();
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
        // int sizeInBytes = 250 * 1024 * 1024;  //250MB


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
    void shouldAShowAllFilmsSortByRanking() {

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/movies/all").queryParam("sort", "ranking").build())
                .exchange().expectStatus().isOk().expectBody().json(response.ranking);
    }

    @Test
    void shouldAShowAllFilmsSortBySize() {

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/movies/all").queryParam("sort", "film_size").build())
                .exchange().expectStatus().isOk();//.expectBody().json(response.ranking);
    }


}
