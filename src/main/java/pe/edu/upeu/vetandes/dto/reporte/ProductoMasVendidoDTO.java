package pe.edu.upeu.vetandes.dto.reporte;

import java.math.BigDecimal;

public record ProductoMasVendidoDTO(
        Long productoId,
        String nombreProducto,
        String nombreCategoria,
        Long cantidadVendida,
        BigDecimal montoTotal
) {
}
