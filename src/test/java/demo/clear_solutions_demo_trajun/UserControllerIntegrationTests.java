package demo.clear_solutions_demo_trajun;

import demo.clear_solutions_demo_trajun.domain.User;
import demo.clear_solutions_demo_trajun.domain.UserUpdateDTO;
import demo.clear_solutions_demo_trajun.repository.UserRepository;
import demo.clear_solutions_demo_trajun.service.UserService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@Testcontainers
@AutoConfigureWebTestClient
public class UserControllerIntegrationTests {

    @Autowired
    public UserControllerIntegrationTests(WebTestClient webClient) {
        this.webTestClient = webClient;
    }

    @Container
    @ServiceConnection
    private final static MongoDBContainer mongoDbContainer = new MongoDBContainer(DockerImageName.parse("mongo:latest"));

    private final WebTestClient webTestClient;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    private final static String apiPath = "/api/users";


    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
    }

    //@Value("${user.minAge}")
    private final int minAge = 18;

    private final LocalDate dateInPast = LocalDate.now().minusYears(minAge * 2L);
    private final LocalDate dateInFuture = LocalDate.now().plusYears(50);
    private final LocalDate dateInPastMinusHalfMinAge = LocalDate.now().minusYears(minAge / 2L);

    String notFoundTemplate = "User with email %s not found";
    String existsTemplate = "User with id %s already exists.";
    String minAgeTemplate = "User must be at least %d years old.".formatted(minAge);


    public User getValidUser() {
        return new User("test@example.com", "John", "Doe", dateInPast, "123 Main St", "1234567890");
    }

    @Test
    void findUserById_test_should_return_user_object_and_status_should_be_ok() {
        User user = getValidUser();
        userService.createUser(user).block();
        String url = String.format("%s/%s", apiPath, user.getEmail());
        webTestClient
                .get()
                .uri(url)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(User.class)
                .consumeWith(result -> {
                    User existUser = result.getResponseBody();
                    assert existUser != null;
                    Assertions.assertEquals(user.getEmail(), existUser.getEmail());
                    Assertions.assertEquals(user.getBirthDate(), existUser.getBirthDate());
                });
    }

    @Test
    void findUserById_user_not_exist_test_should_return_error_message_and_status_should_not_found() {
        String nonExist = "nonexist@mail.com";

        String message = String.format(notFoundTemplate, nonExist);
        String url = String.format("%s/%s", apiPath, nonExist);
        webTestClient
                .get()
                .uri(url)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo(message);
    }

    @Test
    void createUser_test_should_return_user_object_and_status_should_be_ok() {
        User user = getValidUser();
        webTestClient
                .post()
                .uri(apiPath)
                .body(Mono.just(user), User.class)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(User.class)
                .consumeWith(result -> {
                    User existUser = result.getResponseBody();
                    assert existUser != null;
                    Assertions.assertEquals(user.getEmail(), existUser.getEmail());
                    Assertions.assertEquals(user.getBirthDate(), existUser.getBirthDate());
                });
    }

    @Test
    void createUser_test_should_return_error_object_with_invalid_fields_desc_and_expect_status_bad_request() {
        User user = getValidUser();
        user.setEmail(null);
        webTestClient
                .post()
                .uri(apiPath)
                .body(Mono.just(user), User.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.error[0].message").isNotEmpty()
                .jsonPath("$.error[0].field").isEqualTo("email");
    }

    @Test
    void createUser_that_already_exists_test_should_error_object_with_message_and_expect_status_bad_request() {
        User user = getValidUser();
        String message = existsTemplate.formatted(user.getId());
        userService.createUser(user).block();
        webTestClient
                .post()
                .uri(apiPath)
                .body(Mono.just(user), User.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.detail").isEqualTo(message);
    }

    @Test
    void createUser_that_is_lower_than_min_age_test_should_return_error_object_with_message_and_expect_status_bad_request() {
        User user = getValidUser();
        user.setBirthDate(dateInPastMinusHalfMinAge);
        String message = minAgeTemplate.formatted(minAge);
        webTestClient
                .post()
                .uri(apiPath)
                .body(Mono.just(user), User.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.detail").isEqualTo(message);
    }

    @Test
    void updateUserFields_user_is_old_enough() {
        String newName = "Mykola";
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .firstName(Optional.of(newName))
                .address(Optional.empty()) //will nullify(delete) this field
                .build(); //other fields remain same

        User existingUser = getValidUser();
        User updatedUser = getValidUser();
        Util.updateFieldsFromDTO(updatedUser, updateDTO);
        userService.createUser(existingUser).block();
        webTestClient.patch()
                .uri(apiPath + "/" + existingUser.getEmail())
                .body(Mono.just(updateDTO), UserUpdateDTO.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(User.class)
                .consumeWith(result -> {
                    User resUser = result.getResponseBody();
                    assert resUser != null;

                    Assertions.assertEquals(newName, resUser.getFirstName());

                    Assertions.assertNull(resUser.getAddress());

                    Assertions.assertEquals(existingUser.getBirthDate(), resUser.getBirthDate());
                });
    }

    @Test
    void updateUserFields_user_is_not_old_enough_expect_bad_request() {
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .birthDate(Optional.of(dateInPastMinusHalfMinAge))
                .build();


        User existingUser = getValidUser();
        User updatedUser = getValidUser();
        Util.updateFieldsFromDTO(updatedUser, updateDTO);
        String message = minAgeTemplate;
        userService.createUser(existingUser).block();


        webTestClient.patch()
                .uri(apiPath + "/" + existingUser.getEmail())
                .body(Mono.just(updateDTO), UserUpdateDTO.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo(message);
    }

    @Test
    void updateUserFields_user_not_found_expect_user_not_found_exception() {
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .firstName(Optional.of("John"))
                .build();

        String nonExistentEmail = "nonexistent@example.com";

        User updatedUser = getValidUser();
        Util.updateFieldsFromDTO(updatedUser, updateDTO);
        String message = String.format(notFoundTemplate, nonExistentEmail);

        webTestClient.patch()
                .uri(apiPath + "/" + nonExistentEmail)
                .body(Mono.just(updateDTO), UserUpdateDTO.class)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo(message);
    }

    @Test
    void updateUserFields_DTO_is_invalid() {
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .firstName(Optional.of(""))
                .birthDate(Optional.of(dateInFuture))
                .build();

        User existingUser = getValidUser();

        webTestClient.patch()
                .uri(apiPath + "/" + existingUser.getEmail())
                .body(Mono.just(updateDTO), UserUpdateDTO.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.error").isArray()
                .jsonPath("$.error").isNotEmpty()
                .jsonPath("$.error[*].message").isNotEmpty()
                .jsonPath("$.error[*].field", Matchers.contains("birthDate", "firstName"));
    }


    @Test
    void updateUser_user_is_old_enough() {
        User user = getValidUser();
        userService.createUser(user).block();
        user.setAddress("12");
        webTestClient.put()
                .uri(apiPath + "/" + user.getEmail())
                .body(BodyInserters.fromValue(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .consumeWith(result -> {
                    User newUser = result.getResponseBody();
                    assert newUser != null;
                    Assertions.assertEquals(newUser.getAddress(), "12");
                });
    }

    @Test
    void updateUser_user_is_not_old_enough_expect_bad_request() {
        User user = getValidUser();
        userService.createUser(user).block();

        user.setBirthDate(dateInPastMinusHalfMinAge); // User younger than minimum age

        String message = minAgeTemplate;

        webTestClient.put()
                .uri(apiPath + "/" + user.getEmail())
                .body(BodyInserters.fromValue(user))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo(message);
    }

    @Test
    void updateUser_user_invalid_body_expect_bad_request() {
        User user = getValidUser();
        user.setBirthDate(dateInFuture);

        webTestClient.put()
                .uri(apiPath + "/" + user.getEmail())
                .body(BodyInserters.fromValue(user))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.error").isArray()
                .jsonPath("$.error").isNotEmpty()
                .jsonPath("$.error[0].message").isNotEmpty()
                .jsonPath("$.error[0].field", Matchers.contains("birthDate"));
    }

    @Test
    void updateUser_user_email_changed_but_is_already_taken_by_other_expect_bad_request() {
        User user = getValidUser();
        String originalEmail = user.getEmail();
        userService.createUser(user).block();
        User otherUser = getValidUser();
        otherUser.setEmail("other@null.com");
        user.setEmail("other@null.com");
        userService.createUser(otherUser).block();
        String message = "User with email " + otherUser.getId() + " already exists. Cannot update.";


        webTestClient.put()
                .uri(apiPath + "/" +originalEmail)
                .body(BodyInserters.fromValue(user))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo(message);
    }

    @Test
    void deleteUser_user_found_expect_no_content() {
        User user = getValidUser();
        userService.createUser(user).block();

        webTestClient.delete()
                .uri(apiPath + "/" + user.getEmail())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void deleteUser_user_not_found_expect_not_found() {
        String nonExistentEmail = "nonexistent@example.com";

        String message = notFoundTemplate.formatted(nonExistentEmail);

        webTestClient.delete()
                .uri(apiPath + "/" + nonExistentEmail)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo(message);
    }

    @Test
    void findUsersByBirthDateRange_valid_dates_expect_users() {
        User user1 = getValidUser();
        user1.setBirthDate(LocalDate.of(1995, 1, 1));
        User user2 = getValidUser();
        user2.setBirthDate(LocalDate.of(1985, 1, 1));
        user2.setEmail("some@mail.com");
        userService.createUser(user1).block();
        userService.createUser(user2).block();

        webTestClient.get()
                .uri("/api/users/search?fromDate=01-01-1990&toDate=31-12-2000")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .isEqualTo(List.of(user1));
    }

    @Test
    void findUsersByBirthDateRange_toDate_inFuture_expect_bad_request() {
        webTestClient.get()
                .uri("/api/users/search?fromDate=01-01-1990&toDate=31-12-2050")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void findUsersByBirthDateRange_invalid_dates_expect_bad_request() {
        String errorMessage = "toDate must be after fromDate";
        webTestClient.get()
                .uri("/api/users/search?fromDate=01-01-2000&toDate=31-12-1990")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(errorMessage);
    }


}
