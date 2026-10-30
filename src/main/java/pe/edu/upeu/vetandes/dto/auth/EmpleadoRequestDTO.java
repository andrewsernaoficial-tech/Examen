package pe.edu.upeu.vetandes.dto.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmpleadoRequestDTO {
    @NotBlank
    @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener 8 dígitos")
    private String dni;
    @NotBlank
    @Size(max = 100)
    private String nombres;
    @NotBlank
    @Size(max = 100)
    private String apellidos;
    @NotBlank
    @Size(max = 60)
    private String cargo;
    @NotBlank
    @Email
    @Size(max = 150)
    private String email;
    @Size(max = 15)
    private String telefono;
    @NotNull
    private Boolean estado;
}
