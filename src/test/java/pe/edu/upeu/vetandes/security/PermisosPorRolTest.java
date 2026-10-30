package pe.edu.upeu.vetandes.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import pe.edu.upeu.vetandes.entity.Rol;
import pe.edu.upeu.vetandes.enums.Permiso;        // ← paquete correcto

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermisosPorRolTest {

    private static final Rol ADMIN       = new Rol(1L, "ADMIN",       null);
    private static final Rol VETERINARIO = new Rol(2L, "VETERINARIO", null);
    private static final Rol RECEPCION   = new Rol(3L, "RECEPCION",   null);

    private List<String> authorities(Rol... roles) {
        return PermisosPorRol.authorities(List.of(roles)).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    @DisplayName("ADMIN tiene todos los permisos del enum VetAndes")
    void adminTieneTodo() {
        assertThat(PermisosPorRol.de("ADMIN"))
                .containsExactlyInAnyOrder(Permiso.values());
        assertThat(authorities(ADMIN))
                .contains(
                        "ROLE_ADMIN",
                        "REPORTE_VER",
                        "USUARIOS_ADMINISTRAR",
                        "ATENCION_REGISTRAR",
                        "MASCOTA_ESCRIBIR",
                        "PRESTACION_ESCRIBIR");
    }

    @Test
    @DisplayName("VETERINARIO registra atenciones pero no escribe prestaciones ni ve reportes")
    void veterinario() {
        assertThat(authorities(VETERINARIO))
                .contains(
                        "ROLE_VETERINARIO",
                        "ATENCION_LEER",
                        "ATENCION_REGISTRAR",
                        "ATENCION_ANULAR",
                        "MASCOTA_LEER",
                        "PRESTACION_LEER")
                .doesNotContain(
                        "REPORTE_VER",
                        "MASCOTA_ESCRIBIR",
                        "PRESTACION_ESCRIBIR",
                        "USUARIOS_ADMINISTRAR");
    }

    @Test
    @DisplayName("Con dos roles recibe la unión de sus permisos, sin repetidos")
    void variosRoles() {
        assertThat(authorities(VETERINARIO, RECEPCION))
                .contains(
                        "ROLE_VETERINARIO",
                        "ROLE_RECEPCION",
                        "MASCOTA_ESCRIBIR",   // solo RECEPCION
                        "ATENCION_REGISTRAR") // solo VETERINARIO
                .doesNotContain("REPORTE_VER")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Un rol sin permisos definidos solo aporta ROLE_<nombre>")
    void rolDesconocido() {
        assertThat(authorities(new Rol(9L, "AUDITOR", null)))
                .containsExactly("ROLE_AUDITOR");
    }
}