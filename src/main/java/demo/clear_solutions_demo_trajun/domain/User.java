package demo.clear_solutions_demo_trajun.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document("usr")
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "User")
public class User {
    @Email
    @NotBlank
    @Id
    @Schema(description = "Must be valid email")
    private String email;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Past
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Schema( pattern = "dd-MM-yyyy", example = "24-08-1991", type = "string",
            requiredMode = Schema.RequiredMode.REQUIRED, description = "Must be in the past")
    private LocalDate birthDate;

    private String address;

    //10 digits just for example
    @Pattern(regexp = "[0-9]{10}", message = "Phone number must be 10 digits")
    private String phoneNumber;

    @JsonIgnore
    public String getId() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
