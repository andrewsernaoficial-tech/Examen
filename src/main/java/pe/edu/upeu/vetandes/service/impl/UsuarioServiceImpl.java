package pe.edu.upeu.vetandes.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.vetandes.dto.auth.EmpleadoResponseDTO;
import pe.edu.upeu.vetandes.dto.auth.RolResponseDTO;
import pe.edu.upeu.vetandes.dto.auth.UsuarioRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.EmpleadoRequestDTO;
import pe.edu.upeu.vetandes.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.vetandes.entity.Empleado;
import pe.edu.upeu.vetandes.entity.Rol;
import pe.edu.upeu.vetandes.entity.Usuario;
import pe.edu.upeu.vetandes.exception.RecursosNoEncontradoException;
import pe.edu.upeu.vetandes.exception.ReglaNegocioException;
import pe.edu.upeu.vetandes.mapper.UsuarioMapper;
import pe.edu.upeu.vetandes.repository.EmpleadoRepository;
import pe.edu.upeu.vetandes.repository.RolRepository;
import pe.edu.upeu.vetandes.repository.UsuarioRepository;
import pe.edu.upeu.vetandes.service.service.UsuarioService;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final Logger log =
            LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            EmpleadoRepository empleadoRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    @Transactional
    public UsuarioResponseDTO asignarRoles(Long id, Set<Long> rolIds, String username) {
        Usuario usuario = buscarUsuario(id);
        List<Rol> roles = rolRepository.findAllById(rolIds);
        if (roles.size() != rolIds.size()) {
            throw new RecursosNoEncontradoException("Uno o más roles no existen");
        }
        if (usuario.getUsername().equalsIgnoreCase(username)
                && usuario.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getNombre()))
                && roles.stream().noneMatch(r -> "ADMIN".equals(r.getNombre()))) {
            throw new ReglaNegocioException("No puede quitarse a sí mismo el rol ADMIN");
        }
        usuario.setRoles(new HashSet<>(roles));
        usuarioRepository.save(usuario);
        log.info("Roles actualizados: usuario={} administrador={} rolIds={}", usuario.getUsername(), username, rolIds);
        return usuarioMapper.toUsuarioResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO cambiarEstado(Long id, boolean estado, String username) {
        Usuario usuario = buscarUsuario(id);
        if (!estado && usuario.getUsername().equalsIgnoreCase(username)) {
            throw new ReglaNegocioException("No puede desactivarse a sí mismo");
        }
        usuario.setEstado(estado);
        usuarioRepository.save(usuario);
        log.info("Estado actualizado: usuario={} administrador={} estado={}", usuario.getUsername(), username, estado);
        return usuarioMapper.toUsuarioResponse(usuario);
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() ->
                new RecursosNoEncontradoException("Usuario no encontrado con id: " + id));
    }

    @Override
    @Transactional
    public UsuarioResponseDTO crear(UsuarioRequestDTO request) {

        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);

        log.info("Registrando usuario={} empleadoId={} rolIds={}",
                username, request.getEmpleadoId(), request.getRolIds());

        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new RecursosNoEncontradoException(
                        "Empleado no encontrado con id: " + request.getEmpleadoId()));

        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            throw new ReglaNegocioException(
                    "El empleado " + empleado.getNombreCompleto() + " está inactivo");
        }

        if (usuarioRepository.existsByEmpleadoId(empleado.getId())) {
            throw new ReglaNegocioException(
                    "El empleado " + empleado.getNombreCompleto() + " ya tiene un usuario");
        }

        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new ReglaNegocioException(
                    "Ya existe un usuario con el nombre: " + username);
        }

        List<Rol> roles = rolRepository.findAllById(request.getRolIds());
        if (roles.size() != request.getRolIds().size()) {
            Set<Long> encontrados = roles.stream().map(Rol::getId).collect(Collectors.toSet());
            Set<Long> faltantes = new HashSet<>(request.getRolIds());
            faltantes.removeAll(encontrados);
            throw new RecursosNoEncontradoException("Roles no encontrados: " + faltantes);
        }

        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ReglaNegocioException("La contraseña no puede superar 72 bytes");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);

        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEstado(true);
        usuario.setEmpleado(empleado);
        usuario.setRoles(new HashSet<>(roles));

        Usuario guardado = usuarioRepository.save(usuario);

        log.info("Usuario registrado id={} usuario={} roles={}",
                guardado.getId(), username, usuarioMapper.nombresRoles(roles));

        return usuarioMapper.toUsuarioResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll(Sort.by("username"))
                .stream()
                .map(usuarioMapper::toUsuarioResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpleadoResponseDTO> listarEmpleados() {
        Set<Long> conUsuario = usuarioRepository.findEmpleadoIdsConUsuario();

        return empleadoRepository.findAll(Sort.by("apellidos", "nombres"))
                .stream()
                .map(e -> usuarioMapper.toEmpleadoResponse(e, conUsuario.contains(e.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponseDTO> listarRoles() {
        return rolRepository.findAll(Sort.by("nombre"))
                .stream()
                .map(usuarioMapper::toRolResponse)
                .toList();
    }

    @Override
    @Transactional
    public EmpleadoResponseDTO crearEmpleado(EmpleadoRequestDTO request) {
        String dni = request.getDni().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (empleadoRepository.existsByDni(dni)) {
            throw new ReglaNegocioException("Ya existe un empleado con ese DNI");
        }
        if (empleadoRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("Ya existe un empleado con ese correo");
        }
        Empleado empleado = new Empleado();
        empleado.setDni(dni);
        empleado.setNombres(request.getNombres().trim());
        empleado.setApellidos(request.getApellidos().trim());
        empleado.setCargo(request.getCargo().trim());
        empleado.setEmail(email);
        empleado.setTelefono(request.getTelefono() == null ? null : request.getTelefono().trim());
        empleado.setEstado(request.getEstado());
        return usuarioMapper.toEmpleadoResponse(empleadoRepository.save(empleado), false);
    }
}
