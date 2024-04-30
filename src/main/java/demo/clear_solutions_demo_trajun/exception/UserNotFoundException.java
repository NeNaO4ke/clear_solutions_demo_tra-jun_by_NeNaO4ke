package demo.clear_solutions_demo_trajun.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class UserNotFoundException extends Exception {
    public UserNotFoundException(String message)  {
        super(message);
    }

    public static UserNotFoundException fromId(String id)  {
        return new UserNotFoundException(String.format("User with email %s not found", id));
    }
}
