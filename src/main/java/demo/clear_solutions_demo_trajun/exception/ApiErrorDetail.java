package demo.clear_solutions_demo_trajun.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.ProblemDetail;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ApiErrorDetail extends ProblemDetail{
    private List<ApiFieldErrorDetail> error;
    public ApiErrorDetail(ProblemDetail other, List<ApiFieldErrorDetail> errors) {
        super(other);
        this.error = errors;
    }
};
