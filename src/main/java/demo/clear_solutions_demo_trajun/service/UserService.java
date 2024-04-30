package demo.clear_solutions_demo_trajun.service;

import demo.clear_solutions_demo_trajun.Util;
import demo.clear_solutions_demo_trajun.domain.User;
import demo.clear_solutions_demo_trajun.domain.UserUpdateDTO;
import demo.clear_solutions_demo_trajun.exception.UserNotFoundException;
import demo.clear_solutions_demo_trajun.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.Period;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Value("${user.minAge}")
    private int minAge;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> findUserById(String email) {
        return userRepository.findById(email);
    }

    public Mono<User> createUser(User user) {

        if (!isUserOldEnough(user.getBirthDate()))
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User must be at least " + minAge + " years old."));

        return findUserById(user.getId())
                .flatMap(existingUser -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User with id " + existingUser.getId() + " already exists."))
                )
                .switchIfEmpty(userRepository.save(user))
                .cast(User.class);
    }

    public Mono<User> updateUser(User user) {
        return userRepository.save(user);
    }

    public Mono<User> fullyUpdateUser(String userId, User user) {
        return findUserById(userId)
                .flatMap(u -> {
                    if (!isUserOldEnough(user.getBirthDate()))
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "User must be at least " + minAge + " years old."));
                    if (userId.equals(user.getId()))
                        return updateUser(user);
                    else
                        return findUserById(user.getId())
                                .flatMap(ex -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "User with email " + user.getId() + " already exists. Cannot update.")))
                                .switchIfEmpty(updateUser(user))
                                .cast(User.class);
                }).switchIfEmpty(Mono.error(UserNotFoundException.fromId(userId)));
    }

    public Mono<User> updateUserFields(String userId, UserUpdateDTO updateDTO) {
        return findUserById(userId)
                .flatMap(existingUser -> {
                    Util.updateFieldsFromDTO(existingUser, updateDTO);
                    if (isUserOldEnough(existingUser.getBirthDate()))
                        return updateUser(existingUser);
                    else return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "User must be at least " + minAge + " years old."));
                })
                .switchIfEmpty(Mono.error(UserNotFoundException.fromId(userId)));
    }

    public Mono<Void> deleteUser(String userId) {
        return findUserById(userId)
                .switchIfEmpty(Mono.error(UserNotFoundException.fromId(userId)))
                .flatMap(u -> userRepository.deleteById(userId));
    }

    public Flux<User> findUsersByBirthDateRange(LocalDate fromDate, LocalDate toDate) {
        if (toDate.isBefore(fromDate))
            return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "toDate must be after fromDate"));
        return userRepository.findByBirthDateBetween(fromDate, toDate);
    }

    public boolean isUserOldEnough(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears() > minAge;
    }
}

