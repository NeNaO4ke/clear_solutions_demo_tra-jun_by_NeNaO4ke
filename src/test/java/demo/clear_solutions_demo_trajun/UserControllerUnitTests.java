package demo.clear_solutions_demo_trajun;

import demo.clear_solutions_demo_trajun.controller.UserController;
import demo.clear_solutions_demo_trajun.domain.User;
import demo.clear_solutions_demo_trajun.domain.UserUpdateDTO;
import demo.clear_solutions_demo_trajun.exception.UserNotFoundException;
import demo.clear_solutions_demo_trajun.service.UserService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = UserController.class)
@AutoConfigureWebTestClient
public class UserControllerUnitTests {

    private final WebTestClient webTestClient;

    @MockBean
    private final UserService userService;


    //@Value("${user.minAge}")
    private final int minAge = 18;

    private final static String apiPath = "/api/users";

    private final LocalDate dateInPast = LocalDate.now().minusYears(minAge * 2L);
    private final LocalDate dateInFuture = LocalDate.now().plusYears(50);
    private final LocalDate dateInPastMinusHalfMinAge = LocalDate.now().minusYears(minAge / 2L);

    String notFoundTemplate = "User with email %s not found";
    String existsTemplate = "User with id %s already exists.";
    String minAgeTemplate = "User must be at least %d years old.".formatted(minAge);


    public User getValidUser() {
        return new User("test@example.com", "John", "Doe", dateInPast, "123 Main St", "1234567890");
    }

    @Autowired
    public UserControllerUnitTests(WebTestClient webTestClient, UserService userService) {
        this.webTestClient = webTestClient;
        this.userService = userService;
    }

    @Test
    void findUserById_test_should_return_user_object_and_status_should_be_ok() {
        User validUser = getValidUser();
        when(userService.findUserById(validUser.getEmail())).thenReturn(Mono.just(validUser));
        String url = String.format("%s/%s", apiPath, validUser.getEmail());
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
                    Assertions.assertEquals(validUser, existUser);
                });
    }

    @Test
    void findUserById_user_not_exist_test_should_return_error_message_and_status_should_not_found() {
        String nonExist = "nonexist@mail.com";

        String message = String.format(notFoundTemplate, nonExist);
        when(userService.findUserById(nonExist))
                .thenReturn(Mono.error(UserNotFoundException.fromId(nonExist)));
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
        User validUser = getValidUser();
        when(userService.createUser(validUser)).thenReturn(Mono.just(validUser));
        webTestClient
                .post()
                .uri(apiPath)
                .body(Mono.just(validUser), User.class)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(User.class)
                .consumeWith(result -> {
                    User createdUser = result.getResponseBody();
                    assert createdUser != null;
                    Assertions.assertEquals(validUser.getEmail(), createdUser.getEmail());
                    Assertions.assertEquals(validUser.getBirthDate(), createdUser.getBirthDate());
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
        when(userService.createUser(user))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message)));
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
        when(userService.createUser(user))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message)));
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
        when(userService.updateUserFields(existingUser.getEmail(), updateDTO))
                .thenReturn(Mono.just(updatedUser));
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
        when(userService.updateUserFields(existingUser.getEmail(), updateDTO))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message)));


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
        when(userService.updateUserFields(nonExistentEmail, updateDTO))
                .thenReturn(Mono.error(UserNotFoundException.fromId(nonExistentEmail)));

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

        when(userService.fullyUpdateUser(user.getEmail(), user)).thenReturn(Mono.just(user));

        webTestClient.put()
                .uri(apiPath + "/" + user.getEmail())
                .body(BodyInserters.fromValue(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .isEqualTo(user);
    }

    @Test
    void updateUser_user_is_not_old_enough_expect_bad_request() {
        User user = getValidUser();
        user.setBirthDate(dateInPastMinusHalfMinAge); // User younger than minimum age

        String message = minAgeTemplate;
        when(userService.fullyUpdateUser(user.getEmail(), user))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message)));

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
        User otherUser = getValidUser();
        otherUser.setEmail("other@null.com");
        user.setEmail("other@null.com");

        String message = "User with email " + otherUser.getId() + " already exists. Cannot update.";
        when(userService.fullyUpdateUser(originalEmail, user))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message)));

        webTestClient.put()
                .uri(apiPath + "/" + originalEmail)
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

        when(userService.deleteUser(user.getEmail())).thenReturn(Mono.empty());

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
        when(userService.deleteUser(nonExistentEmail))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, message)));

        webTestClient.delete()
                .uri(apiPath + "/" + nonExistentEmail)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo(message);
    }

    @Test
    void findUsersByBirthDateRange_valid_dates_expect_users() {
        LocalDate fromDate = LocalDate.of(1990, 1, 1);
        LocalDate toDate = LocalDate.of(2000, 12, 31);

        User user1 = getValidUser();
        user1.setBirthDate(LocalDate.of(1995, 1, 1));
        User user2 = getValidUser();
        user2.setBirthDate(LocalDate.of(1985, 1, 1));
        user2.setEmail("some@mail.com");

        when(userService.findUsersByBirthDateRange(fromDate, toDate)).thenReturn(Flux.just(user1));

        webTestClient.get()
                .uri("/api/users/search?fromDate=01-01-1990&toDate=31-12-2000")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .isEqualTo(List.of(user1));
    }

    @Test
    void findUsersByBirthDateRange_toDate_inFuture_expect_bad_request() {
        LocalDate fromDate = LocalDate.of(1990, 1, 1);
        LocalDate toDate = LocalDate.of(2050, 12, 31);

        webTestClient.get()
                .uri("/api/users/search?fromDate=01-01-1990&toDate=31-12-2050")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void findUsersByBirthDateRange_invalid_dates_expect_bad_request() {
        LocalDate fromDate = LocalDate.of(2000, 1, 1);
        LocalDate toDate = LocalDate.of(1990, 12, 31);

        String errorMessage = "toDate must be after fromDate";
        when(userService.findUsersByBirthDateRange(fromDate, toDate))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage));

        webTestClient.get()
                .uri("/api/users/search?fromDate=01-01-2000&toDate=31-12-1990")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(errorMessage);
    }

}
