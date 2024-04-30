package demo.clear_solutions_demo_trajun;

import java.lang.reflect.Field;
import java.util.Optional;

public class Util {
    public static void updateFieldsFromDTO(Object original, Object updateDTO) {
        Field[] fields = updateDTO.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object optional = field.get(updateDTO);
                if (optional != null && (optional instanceof Optional<?>)) {
                    String fieldName = field.getName();
                    Field originalField = original.getClass().getDeclaredField(fieldName);
                    originalField.setAccessible(true);
                    Object value = ((Optional<?>) optional).isPresent() ? ((Optional<?>) optional).get() : null;
                    originalField.set(original, value);
                } else {
                    // Field is null or not Optional class
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // Handle exception
                e.printStackTrace();
            }
        }
    }

}
