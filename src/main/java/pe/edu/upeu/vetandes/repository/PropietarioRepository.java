package pe.edu.upeu.vetandes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.vetandes.entity.Propietario;
import java.util.Optional;

public interface PropietarioRepository extends JpaRepository<Propietario, Long> {
    Optional<Propietario> findByDni(String dni);
}