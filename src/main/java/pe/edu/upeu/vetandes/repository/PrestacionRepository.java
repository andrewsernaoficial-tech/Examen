package pe.edu.upeu.vetandes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.vetandes.entity.Prestacion;
import java.util.Optional;

public interface PrestacionRepository extends JpaRepository<Prestacion, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);
    Optional<Prestacion> findByCodigo(String codigo);
}