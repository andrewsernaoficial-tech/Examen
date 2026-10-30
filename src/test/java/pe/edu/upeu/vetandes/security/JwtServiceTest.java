package pe.edu.upeu.vetandes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import pe.edu.upeu.vetandes.entity.Rol;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO =
            "w9Z6dP/dzykNH/uiI3Z/KGjBlP0H4Ji+Tuq4fm2cTNuLD4jWafu1gBL74V2SJEvy";

    private static final String OTRO_SECRETO =
            "q8Bx3mN0vRk7TzYp1LcW5aHs9JdFg2Ue4OiPlKjMnBvCxZaSdFgHjKlQwErTyUiO";

    private final JwtService jwtService = new JwtService(SECRETO, 60);

    /** Simula un VETERINARIO que puede registrar atenciones pero no ver reportes */
    private UserDetails veterinario() {
        return User.withUsername("dr.silva")
                .password("no-importa")
                .authorities(PermisosPorRol.authorities(
                        List.of(new Rol(2L, "VETERINARIO", null))))
                .build();
    }

    @Test
    @DisplayName("El token generado contiene el username y las authorities de VetAndes")
    void generarYLeerToken() {
        String token = jwtService.generarToken(veterinario());

        Claims claims = jwtService.leerClaims(token);

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(claims.getSubject()).isEqualTo("dr.silva");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());

        List<?> authorities = claims.get("authorities", List.class);
        assertThat(authorities.stream().map(Object::toString).toList())
                .contains("ROLE_VETERINARIO", "ATENCION_REGISTRAR", "MASCOTA_LEER")
                .doesNotContain("REPORTE_VER", "MASCOTA_ESCRIBIR");

        assertThat(jwtService.extraerUsername(token)).isEqualTo("dr.silva");
    }

    @Test
    @DisplayName("Un token alterado no pasa la verificación de firma")
    void tokenAlterado() {
        String token = jwtService.generarToken(veterinario());
        String alterado = token.substring(0, token.length() - 2)
                + (token.endsWith("AA") ? "BB" : "AA");

        assertThatThrownBy(() -> jwtService.extraerUsername(alterado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Un token firmado con otra clave es rechazado")
    void tokenDeOtraClave() {
        String ajeno = new JwtService(OTRO_SECRETO, 60).generarToken(veterinario());

        assertThatThrownBy(() -> jwtService.extraerUsername(ajeno))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Un token expirado lanza ExpiredJwtException")
    void tokenExpirado() {
        String vencido = new JwtService(SECRETO, -1).generarToken(veterinario());

        assertThatThrownBy(() -> jwtService.extraerUsername(vencido))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("La expiración se informa en segundos")
    void expiracionEnSegundos() {
        assertThat(jwtService.getExpiracionSegundos()).isEqualTo(3600);
    }
}