package pe.edu.upeu.vetandes.service.service;

import pe.edu.upeu.vetandes.dto.auth.EmpleadoResponseDTO;
import pe.edu.upeu.vetandes.dto.auth.RolResponseDTO;
import pe.edu.upeu.vetandes.dto.auth.UsuarioRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.EmpleadoRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.UsuarioResponseDTO;
import java.util.List;
import java.util.Set;

public interface UsuarioService {

    UsuarioResponseDTO asignarRoles(Long id, Set<Long> rolIds, String username);

    UsuarioResponseDTO cambiarEstado(Long id, boolean estado, String username);

    UsuarioResponseDTO crear(UsuarioRequestDTO request);

    List<UsuarioResponseDTO> listar();

    List<EmpleadoResponseDTO> listarEmpleados();

    EmpleadoResponseDTO crearEmpleado(EmpleadoRequestDTO request);

    List<RolResponseDTO> listarRoles();
}