package demo.clear_solutions_demo_trajun.repository;


import demo.clear_solutions_demo_trajun.domain.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@EnableReactiveMongoRepositories
public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Flux<User> findByBirthDateBetween(LocalDate fromDate, LocalDate toDate);

}

