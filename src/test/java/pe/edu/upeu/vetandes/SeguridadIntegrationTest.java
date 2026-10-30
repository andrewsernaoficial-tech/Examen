package pe.edu.upeu.vetandes;

import pe.edu.upeu.vetandes.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SeguridadIntegrationTest {
    @LocalServerPort int port;
    @Autowired JwtService jwt;
    @org.springframework.beans.factory.annotation.Value("${app.jwt.secret}") String secret;
    final HttpClient client = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();
    record Respuesta(int status, JsonNode body) {}

    Respuesta request(String method, String path, String body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Respuesta(response.statusCode(), response.body().isBlank() || !response.headers().firstValue("Content-Type").orElse("").contains("application/json") ? null : json.readTree(response.body()));
    }

    Respuesta login(String username, String password) throws Exception {
        return request("POST", "/api/v1/auth/login",
                json.writeValueAsString(java.util.Map.of("username", username, "password", password)), null);
    }

    String token(String username, String password) throws Exception {
        var response = login(username, password);
        assertEquals(200, response.status());
        return response.body().get("token").asText();
    }

    @Test void rutasPublicasYPrivadas() throws Exception {
        assertEquals(200, request("GET", "/api/health", null, null).status());
        var response = request("GET", "/api/v1/productos", null, null);
        assertEquals(401, response.status());
        assertEquals("Unauthorized", response.body().get("error").asText());
        assertEquals("/api/v1/productos", response.body().get("path").asText());
    }

    @ParameterizedTest
    @CsvSource({"admin,incorrecta", "inactivo,Inactivo2026*", "noexiste,Incorrecta123"})
    void loginFallidoNoRevelaCuenta(String username, String password) throws Exception {
        var response = login(username, password);
        assertEquals(401, response.status());
        assertEquals("Usuario o contraseña incorrectos", response.body().get("message").asText());
    }

    @Test void validaLoginYDatosDelToken() throws Exception {
        var invalid = login("", "Vendedor2026*");
        assertEquals(400, invalid.status());
        assertTrue(invalid.body().get("validationErrors").has("username"));
        String token = token("vendedor", "Vendedor2026*");
        var claims = jwt.leerClaims(token);
        assertEquals("vendedor", claims.getSubject());
        assertEquals(3600000L, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
        var me = request("GET", "/api/v1/auth/me", null, token);
        assertEquals(200, me.status());
        assertFalse(me.body().has("password"));
        assertFalse(me.body().toString().contains("$2a$"));
    }

    @ParameterizedTest
    @CsvSource({
        "vendedor,Vendedor2026*,GET,/api/v1/productos,200",
        "vendedor,Vendedor2026*,DELETE,/api/v1/productos/1,403",
        "vendedor,Vendedor2026*,GET,/api/v1/reportes/ventas-por-categoria,403",
        "almacen,Almacen2026*,POST,/api/v1/ventas,403",
        "almacen,Almacen2026*,GET,/api/v1/clientes,403",
        "admin,Admin2026*,GET,/api/v1/reportes/ventas-por-categoria,200",
        "supervisor,Supervisor2026*,GET,/api/v1/reportes/ventas-por-categoria,403",
        "vendedor,Vendedor2026*,GET,/api/v1/usuarios,403",
        "admin,Admin2026*,GET,/api/v1/empleados,200",
        "admin,Admin2026*,GET,/api/v1/roles,200"
    })
    void permisosPorOperacion(String user, String password, String method, String path, int status) throws Exception {
        var response = request(method, path, method.equals("POST") ? "{}" : null, token(user, password));
        assertEquals(status, response.status());
        if (status == 403) assertEquals("Forbidden", response.body().get("error").asText());
    }

    @Test void rechazaTokenAlteradoExpiradoYDeCuentaInactiva() throws Exception {
        String valid = token("vendedor", "Vendedor2026*");
        String altered = valid.substring(0, valid.lastIndexOf('.') + 1) + "AAAA";
        assertEquals(401, request("GET", "/api/v1/productos", null, altered).status());
        String expired = new JwtService(secret, -1).generarToken(User.withUsername("vendedor")
                .password("test").authorities("ROLE_VENDEDOR").build());
        var expiredResponse = request("GET", "/api/v1/productos", null, expired);
        assertEquals(401, expiredResponse.status());
        assertTrue(expiredResponse.body().get("message").asText().contains("expirado"));
        String inactive = jwt.generarToken(User.withUsername("inactivo").password("test")
                .authorities("ROLE_VENDEDOR").build());
        assertEquals(401, request("GET", "/api/v1/productos", null, inactive).status());
    }

    @Test void supervisorSumaPermisos() throws Exception {
        var response = login("supervisor", "Supervisor2026*");
        assertEquals(200, response.status());
        assertEquals(2, response.body().get("roles").size());
        String permissions = response.body().get("permisos").toString();
        assertTrue(permissions.contains("CATALOGO_ESCRIBIR"));
        assertTrue(permissions.contains("VENTAS_REGISTRAR"));
        assertFalse(permissions.contains("REPORTES_VER"));
    }

    @Test void creaUsuarioYRechazaEmpleadoRepetidoORolInexistente() throws Exception {
        String admin = token("admin", "Admin2026*");
        var employees = request("GET", "/api/v1/empleados", null, admin).body();
        long employee = -1;
        for (JsonNode node : employees) if (!node.get("tieneUsuario").asBoolean()) employee = node.get("id").asLong();
        assertTrue(employee > 0);
        String username = "cajera_" + UUID.randomUUID().toString().substring(0, 8);
        String invalid = "{\"username\":\"" + username + "\",\"password\":\"Cajera2026\",\"empleadoId\":" + employee + ",\"rolIds\":[999]}";
        assertEquals(404, request("POST", "/api/v1/usuarios", invalid, admin).status());
        long role = -1;
        for (JsonNode node : request("GET", "/api/v1/roles", null, admin).body())
            if (node.get("nombre").asText().equals("VENDEDOR")) role = node.get("id").asLong();
        String body = invalid.replace("[999]", "[" + role + "]");
        var created = request("POST", "/api/v1/usuarios", body, admin);
        assertEquals(201, created.status());
        assertFalse(created.body().has("password"));
        assertEquals(200, login(username, "Cajera2026").status());
        assertEquals(409, request("POST", "/api/v1/usuarios", body.replace(username, username + "2"), admin).status());
    }

    @Test void corsPermitePreflightConAuthorizationYPatch() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/ventas/1/anular"))
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("http://localhost:4200", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertTrue(response.headers().firstValue("Access-Control-Allow-Methods").orElseThrow().contains("PATCH"));
    }

    @Test void administraEmpleadosYPermiteProbarRolInexistente() throws Exception {
        String admin = token("admin", "Admin2026*");
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String body = "{\"dni\":\"" + String.format("%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10000000,99999999))
                + "\",\"nombres\":\"Luis\",\"apellidos\":\"Perez\",\"cargo\":\"Vendedor\",\"email\":\"" + uid + "@test.pe\",\"estado\":true}";
        assertEquals(403, request("POST", "/api/v1/empleados", body, token("vendedor", "Vendedor2026*")).status());
        assertEquals(400, request("POST", "/api/v1/empleados", "{}", admin).status());
        var created = request("POST", "/api/v1/empleados", body, admin);
        assertEquals(201, created.status());
        assertFalse(created.body().get("tieneUsuario").asBoolean());
        assertEquals(409, request("POST", "/api/v1/empleados", body, admin).status());
        String invalid = "{\"username\":\"prueba_" + uid + "\",\"password\":\"Prueba2026\",\"empleadoId\":"
                + created.body().get("id").asLong() + ",\"rolIds\":[999]}";
        var response = request("POST", "/api/v1/usuarios", invalid, admin);
        assertEquals(404, response.status());
        assertTrue(response.body().get("message").asText().contains("999"));
    }

    record Cuenta(long id, String username, String token) {}

    Cuenta nuevaCuenta(String admin) throws Exception {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String empleado = json.writeValueAsString(java.util.Map.of(
                "dni", String.format("%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10000000, 99999999)),
                "nombres", "Prueba", "apellidos", "Autonoma", "cargo", "Vendedor",
                "email", uid + "@test.pe", "estado", true));
        var creado = request("POST", "/api/v1/empleados", empleado, admin);
        assertEquals(201, creado.status());
        String username = "s10_" + uid;
        var usuario = request("POST", "/api/v1/usuarios", json.writeValueAsString(java.util.Map.of(
                "username", username, "password", "Inicial2026", "empleadoId", creado.body().get("id").asLong(),
                "rolIds", java.util.List.of(rolId(admin, "VENDEDOR")))), admin);
        assertEquals(201, usuario.status());
        return new Cuenta(usuario.body().get("id").asLong(), username, token(username, "Inicial2026"));
    }

    long rolId(String admin, String nombre) throws Exception {
        for (var rol : request("GET", "/api/v1/roles", null, admin).body())
            if (nombre.equals(rol.get("nombre").asText())) return rol.get("id").asLong();
        throw new AssertionError("Rol no encontrado: " + nombre);
    }

    @Test void cambioPasswordValidaYConservaTokenAnterior() throws Exception {
        String admin = token("admin", "Admin2026*");
        Cuenta cuenta = nuevaCuenta(admin);
        String ruta = "/api/v1/auth/password";
        String valido = "{\"passwordActual\":\"Inicial2026\",\"passwordNueva\":\"Nueva2026\"}";
        assertEquals(401, request("PUT", ruta, valido, null).status());
        for (String nueva : java.util.List.of("corta1", "abcdefgh", "12345678", "A1".repeat(37))) {
            assertEquals(400, request("PUT", ruta, json.writeValueAsString(java.util.Map.of(
                    "passwordActual", "Inicial2026", "passwordNueva", nueva)), cuenta.token()).status());
        }
        var incorrecta = request("PUT", ruta, valido.replace("Inicial2026", "Incorrecta2026"), cuenta.token());
        assertEquals(409, incorrecta.status());
        assertEquals("La contraseña actual no es correcta", incorrecta.body().get("message").asText());
        var igual = request("PUT", ruta, valido.replace("Nueva2026", "Inicial2026"), cuenta.token());
        assertEquals(409, igual.status());
        assertEquals("La nueva contraseña debe ser distinta de la actual", igual.body().get("message").asText());
        var cambiada = request("PUT", ruta, valido, cuenta.token());
        assertEquals(204, cambiada.status());
        assertNull(cambiada.body());
        assertEquals(401, login(cuenta.username(), "Inicial2026").status());
        assertEquals(200, login(cuenta.username(), "Nueva2026").status());
        assertEquals(200, request("GET", "/api/v1/auth/me", null, cuenta.token()).status());
    }

    @Test void rolesSeReemplazanYAplicanConElMismoToken() throws Exception {
        String admin = token("admin", "Admin2026*");
        Cuenta cuenta = nuevaCuenta(admin);
        String ruta = "/api/v1/usuarios/" + cuenta.id() + "/roles";
        String roles = json.writeValueAsString(java.util.Map.of("rolIds", java.util.List.of(rolId(admin, "ALMACENERO"))));
        assertEquals(403, request("PUT", ruta, roles, cuenta.token()).status());
        assertEquals(400, request("PUT", ruta, "{\"rolIds\":[]}", admin).status());
        assertEquals(400, request("PUT", ruta, "{\"rolIds\":[null]}", admin).status());
        assertEquals(404, request("PUT", ruta, "{\"rolIds\":[999999]}", admin).status());
        assertEquals(404, request("PUT", "/api/v1/usuarios/999999/roles", roles, admin).status());
        long adminId = request("GET", "/api/v1/auth/me", null, admin).body().get("id").asLong();
        assertEquals(409, request("PUT", "/api/v1/usuarios/" + adminId + "/roles", roles, admin).status());
        assertEquals(200, request("GET", "/api/v1/clientes", null, cuenta.token()).status());
        assertEquals(403, request("POST", "/api/v1/categorias", "{}", cuenta.token()).status());
        var actualizada = request("PUT", ruta, roles, admin);
        assertEquals(200, actualizada.status());
        assertEquals(1, actualizada.body().get("roles").size());
        assertFalse(actualizada.body().has("password"));
        assertEquals(403, request("GET", "/api/v1/clientes", null, cuenta.token()).status());
        // El DTO inválido llega al controlador: ya tiene el nuevo permiso de escritura.
        assertEquals(400, request("POST", "/api/v1/categorias", "{}", cuenta.token()).status());
    }

    @Test void estadoBloqueaTokenExistenteYPermiteReactivar() throws Exception {
        String admin = token("admin", "Admin2026*");
        Cuenta cuenta = nuevaCuenta(admin);
        String ruta = "/api/v1/usuarios/" + cuenta.id() + "/estado";
        String desactivar = "{\"estado\":false}";
        assertEquals(403, request("PATCH", ruta, desactivar, cuenta.token()).status());
        assertEquals(400, request("PATCH", ruta, "{}", admin).status());
        assertEquals(400, request("PATCH", ruta, "{\"estado\":null}", admin).status());
        assertEquals(404, request("PATCH", "/api/v1/usuarios/999999/estado", desactivar, admin).status());
        long adminId = request("GET", "/api/v1/auth/me", null, admin).body().get("id").asLong();
        assertEquals(409, request("PATCH", "/api/v1/usuarios/" + adminId + "/estado", desactivar, admin).status());
        var desactivado = request("PATCH", ruta, desactivar, admin);
        assertEquals(200, desactivado.status());
        assertFalse(desactivado.body().get("estado").asBoolean());
        var bloqueado = request("GET", "/api/v1/auth/me", null, cuenta.token());
        assertEquals(401, bloqueado.status());
        assertEquals("El usuario está deshabilitado", bloqueado.body().get("message").asText());
        var loginInactivo = login(cuenta.username(), "Inicial2026");
        assertEquals(401, loginInactivo.status());
        assertEquals("Usuario o contraseña incorrectos", loginInactivo.body().get("message").asText());
        assertEquals(200, request("PATCH", ruta, "{\"estado\":true}", admin).status());
        assertEquals(200, login(cuenta.username(), "Inicial2026").status());
    }
}
