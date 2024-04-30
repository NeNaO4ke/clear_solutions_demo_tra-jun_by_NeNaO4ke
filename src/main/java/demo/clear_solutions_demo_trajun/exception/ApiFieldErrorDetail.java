package demo.clear_solutions_demo_trajun.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ApiFieldErrorDetail {
    private String field, objectName, message;
}
