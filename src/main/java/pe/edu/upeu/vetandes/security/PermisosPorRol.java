package pe.edu.upeu.vetandes.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.edu.upeu.vetandes.entity.Rol;
import pe.edu.upeu.vetandes.enums.Permiso;          // ← paquete correcto

import java.util.*;

public final class PermisosPorRol {

    // ── Matriz VetAndes (Versión B) ─────────────────────────────────────────
    private static final Map<String, Set<Permiso>> PERMISOS = Map.of(

            "ADMIN",
            Collections.unmodifiableSet(EnumSet.allOf(Permiso.class)),

            "VETERINARIO",
            Collections.unmodifiableSet(EnumSet.of(
                    Permiso.ATENCION_LEER,
                    Permiso.ATENCION_REGISTRAR,
                    Permiso.ATENCION_ANULAR,
                    Permiso.MASCOTA_LEER,
                    Permiso.PRESTACION_LEER)),

            "RECEPCION",
            Collections.unmodifiableSet(EnumSet.of(
                    Permiso.MASCOTA_LEER,
                    Permiso.MASCOTA_ESCRIBIR,
                    Permiso.PRESTACION_LEER))
    );

    private PermisosPorRol() {}

    public static Set<Permiso> de(String nombreRol) {
        return PERMISOS.getOrDefault(nombreRol, Set.of());
    }

    public static List<String> permisos(Collection<Rol> roles) {
        Set<String> nombres = new TreeSet<>();
        roles.forEach(rol -> de(rol.getNombre())
                .forEach(permiso -> nombres.add(permiso.name())));
        return new ArrayList<>(nombres);
    }

    public static List<GrantedAuthority> authorities(Collection<Rol> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.stream()
                .map(Rol::getNombre)
                .sorted()
                .forEach(nombre ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + nombre)));
        permisos(roles).forEach(permiso ->
                authorities.add(new SimpleGrantedAuthority(permiso)));
        return authorities;
    }
}