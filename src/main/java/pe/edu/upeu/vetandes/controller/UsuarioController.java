package pe.edu.upeu.vetandes.controller;

import pe.edu.upeu.vetandes.dto.auth.AsignarRolesRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.CambiarEstadoRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.EmpleadoResponseDTO;
import pe.edu.upeu.vetandes.dto.auth.RolResponseDTO;
import pe.edu.upeu.vetandes.dto.auth.UsuarioRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.EmpleadoRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.vetandes.service.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PutMapping("/usuarios/{id}/roles")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioResponseDTO> asignarRoles(
            @PathVariable Long id,
            @Valid @RequestBody AsignarRolesRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(usuarioService.asignarRoles(id, request.getRolIds(), authentication.getName()));
    }

    @PatchMapping("/usuarios/{id}/estado")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, request.getEstado(), authentication.getName()));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponseDTO> crear(
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuarioService.crear(request));
    }

    @GetMapping("/empleados")
    public ResponseEntity<List<EmpleadoResponseDTO>> empleados() {
        return ResponseEntity.ok(usuarioService.listarEmpleados());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RolResponseDTO>> roles() {
        return ResponseEntity.ok(usuarioService.listarRoles());
    }

    @PostMapping("/empleados")
    public ResponseEntity<EmpleadoResponseDTO> crearEmpleado(
            @Valid @RequestBody EmpleadoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearEmpleado(request));
    }
}