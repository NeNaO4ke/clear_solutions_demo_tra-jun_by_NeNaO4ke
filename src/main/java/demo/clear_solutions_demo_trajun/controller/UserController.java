package demo.clear_solutions_demo_trajun.controller;

import demo.clear_solutions_demo_trajun.domain.User;
import demo.clear_solutions_demo_trajun.domain.UserUpdateDTO;
import demo.clear_solutions_demo_trajun.exception.UserNotFoundException;
import demo.clear_solutions_demo_trajun.service.UserService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{email}")
    public Mono<User> findUsersByBirthDateRange(@PathVariable String email) {
        return userService.findUserById(email)
                .switchIfEmpty(Mono.error(UserNotFoundException.fromId(email)));
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public Mono<User> createUser(@Valid @RequestBody User user) {
        return userService.createUser(user);
    }

    @PatchMapping("/{email}")
    public Mono<User> updateUserFields(@PathVariable String email, @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        return userService.updateUserFields(email, userUpdateDTO);
    }

    @PutMapping("/{email}")
    public Mono<User> updateUser(@PathVariable String email, @Valid @RequestBody User user) {
        return userService.fullyUpdateUser(email, user);
    }

    @DeleteMapping("/{email}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(@PathVariable String email) {
        return userService.deleteUser(email);
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<User> findUsersByBirthDateRange(@DateTimeFormat(pattern = "dd-MM-yyyy")
                                                @Schema(pattern = "dd-MM-yyyy", example = "24-08-1991", type = "string",
                                                        description = "Must be before toDate")
                                                @RequestParam LocalDate fromDate,
                                                @Schema(pattern = "dd-MM-yyyy", example = "30-01-2024", type = "string",
                                                        description = "Must be in past and after fromDate")
                                                @DateTimeFormat(pattern = "dd-MM-yyyy") @Past @RequestParam LocalDate toDate) {
        return userService.findUsersByBirthDateRange(fromDate, toDate);
    }
}

