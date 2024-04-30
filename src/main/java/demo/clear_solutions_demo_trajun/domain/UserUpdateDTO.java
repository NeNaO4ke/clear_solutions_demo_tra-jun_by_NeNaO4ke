package demo.clear_solutions_demo_trajun.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Schema(description = "Each field is optional. Pass field with null value to delete field if possible. Don`t pass field to keep it as it is now.")
public class UserUpdateDTO {

    private Optional<@NotBlank String> firstName;

    private Optional<@NotBlank String> lastName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Schema(pattern = "dd-MM-yyyy", example = "24-08-1991", type = "string",
            requiredMode = Schema.RequiredMode.REQUIRED, description = "Must be in the past")
    private Optional<@Past @NotNull LocalDate> birthDate;

    private Optional<String> address;

    private Optional<@Pattern(regexp = "[0-9]{10}", message = "Phone number must be 10 digits") String> phoneNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserUpdateDTO that = (UserUpdateDTO) o;
        return Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(birthDate, that.birthDate) && Objects.equals(address, that.address) && Objects.equals(phoneNumber, that.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, birthDate, address, phoneNumber);
    }
}
