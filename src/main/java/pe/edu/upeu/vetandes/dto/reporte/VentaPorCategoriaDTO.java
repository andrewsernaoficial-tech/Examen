package pe.edu.upeu.vetandes.dto.reporte;

import java.math.BigDecimal;

public record VentaPorCategoriaDTO(
        Long categoriaId,
        String nombreCategoria,
        Long cantidadTotal,
        BigDecimal montoTotal
) {
}
