package pe.edu.upeu.vetandes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.vetandes.dto.reporte.AtencionPorEspecieDTO;
import pe.edu.upeu.vetandes.entity.Atencion;
import pe.edu.upeu.vetandes.enums.EstadoAtencion;

import java.time.LocalDateTime;
import java.util.List;

public interface AtencionRepository extends JpaRepository<Atencion, Long> {

    @Query("SELECT new pe.edu.upeu.vetandes.dto.reporte.AtencionPorEspecieDTO(" +
            "  m.especie, " +
            "  COUNT(a), " +
            "  SUM(a.total) " +
            ") " +
            "FROM Atencion a " +
            "JOIN a.mascota m " +
            "WHERE a.estado != :estadoExcluido " +
            "  AND a.fechaHora BETWEEN :fechaInicio AND :fechaFin " +
            "GROUP BY m.especie " +
            "ORDER BY SUM(a.total) DESC")
    List<AtencionPorEspecieDTO> obtenerReporteAtencionesPorEspecie(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("estadoExcluido") EstadoAtencion estadoExcluido
    );

    long countByMascotaId(Long mascotaId);
}