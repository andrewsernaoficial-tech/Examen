package pe.edu.upeu.vetandes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.vetandes.entity.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {
}
