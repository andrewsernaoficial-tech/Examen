package pe.edu.upeu.vetandes.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarEstadoRequestDTO {
    @NotNull(message = "El estado es obligatorio")
    private Boolean estado;
}
