package pe.edu.upeu.vetandes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import pe.edu.upeu.vetandes.enums.Permiso;                    // ← paquete correcto
import pe.edu.upeu.vetandes.security.JwtAuthenticationFilter;
import pe.edu.upeu.vetandes.security.JwtService;
import pe.edu.upeu.vetandes.security.RestAccessDeniedHandler;
import pe.edu.upeu.vetandes.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/api/v1/auth/login",
            "/api/health",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
    };

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            CorsConfigurationSource corsConfigurationSource,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Rutas públicas ─────────────────────────────────────────
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── Prestaciones ───────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,  "/api/v1/prestaciones/**")
                        .hasAuthority(Permiso.PRESTACION_LEER.name())
                        .requestMatchers("/api/v1/prestaciones/**")
                        .hasAuthority(Permiso.PRESTACION_ESCRIBIR.name())

                        // ── Mascotas ───────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/mascotas/**",
                                "/api/v1/propietarios/**")
                        .hasAuthority(Permiso.MASCOTA_LEER.name())
                        .requestMatchers("/api/v1/mascotas/**", "/api/v1/propietarios/**")
                        .hasAuthority(Permiso.MASCOTA_ESCRIBIR.name())

                        // ── Atenciones ─────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/v1/atenciones")
                        .hasAuthority(Permiso.ATENCION_REGISTRAR.name())
                        .requestMatchers(HttpMethod.GET, "/api/v1/atenciones/**")
                        .hasAuthority(Permiso.ATENCION_LEER.name())
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/atenciones/*/anular")
                        .hasAuthority(Permiso.ATENCION_ANULAR.name())

                        // ── Reportes ───────────────────────────────────────────────
                        .requestMatchers("/api/v1/reportes/**")
                        .hasAuthority(Permiso.REPORTE_VER.name())

                        // ── Usuarios / Roles / Empleados ───────────────────────────
                        .requestMatchers("/api/v1/usuarios/**",
                                "/api/v1/empleados/**",
                                "/api/v1/roles/**")
                        .hasAuthority(Permiso.USUARIOS_ADMINISTRAR.name())

                        .anyRequest().authenticated())

                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}