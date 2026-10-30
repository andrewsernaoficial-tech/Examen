package pe.edu.upeu.vetandes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.vetandes.entity.Empleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    boolean existsByDni(String dni);
    boolean existsByEmailIgnoreCase(String email);
}
