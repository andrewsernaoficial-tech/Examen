package pe.edu.upeu.vetandes.service.service;
import pe.edu.upeu.vetandes.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.vetandes.dto.reporte.VentaPorCategoriaDTO;

import java.time.LocalDate;
import java.util.List;

public interface ReporteService {
    List<VentaPorCategoriaDTO> ventasPorCategoria(
            LocalDate desde,
            LocalDate hasta);

    List<ProductoMasVendidoDTO> productosMasVendidos(
            LocalDate desde,
            LocalDate hasta);
}
