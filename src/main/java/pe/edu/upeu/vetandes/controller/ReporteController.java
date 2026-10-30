package pe.edu.upeu.vetandes.controller;

import pe.edu.upeu.vetandes.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.vetandes.dto.reporte.VentaPorCategoriaDTO;
import pe.edu.upeu.vetandes.service.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('REPORTES_VER')")
@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {
    private final ReporteService reporteService;

    public ReporteController(
            ReporteService reporteService) {
        this.reporteService = reporteService;
    }


    @GetMapping("/ventas-por-categoria")
    public ResponseEntity<List<VentaPorCategoriaDTO>> ventasPorCategoria(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {

        return ResponseEntity.ok(
                reporteService.ventasPorCategoria(desde, hasta)
        );
    }


    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<List<ProductoMasVendidoDTO>> productosMasVendidos(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {

        return ResponseEntity.ok(
                reporteService.productosMasVendidos(desde, hasta)
        );
    }
}
