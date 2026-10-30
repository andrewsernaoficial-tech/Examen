package pe.edu.upeu.vetandes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.vetandes.entity.Mascota;
import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {
    boolean existsByNombreIgnoreCaseAndPropietarioId(String nombre, Long propietarioId);
    List<Mascota> findByPropietarioId(Long propietarioId);
}