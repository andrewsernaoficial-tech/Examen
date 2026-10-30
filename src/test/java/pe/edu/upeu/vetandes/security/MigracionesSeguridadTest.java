package pe.edu.upeu.vetandes.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MigracionesSeguridadTest {
    @Test void esquemaYUsuariosInicialesSonConsistentes() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:migraciones;MODE=Oracle", "sa", "")) {
            for (String file : new String[]{"V1__esquema_inicial.sql", "V2__seguridad.sql"}) {
                String sql = new ClassPathResource("db/migration/" + file)
                        .getContentAsString(StandardCharsets.UTF_8)
                        .replaceAll("(?i)varchar2\\((\\d+) char\\)", "varchar2($1)")
                        .replace("systimestamp", "CURRENT_TIMESTAMP")
                        .replaceAll("(?i)(upper|lower)\\((nombre|email|username)\\)", "$2");
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
            }
            var passwords = Map.of("admin", "Admin2026*", "vendedor", "Vendedor2026*",
                    "almacen", "Almacen2026*", "supervisor", "Supervisor2026*", "inactivo", "Inactivo2026*");
            var encoder = new BCryptPasswordEncoder();
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("select username, password from usuarios")) {
                int count = 0;
                while (rows.next()) {
                    assertTrue(encoder.matches(passwords.get(rows.getString(1)), rows.getString(2)));
                    count++;
                }
                assertEquals(5, count);
            }
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("select count(*) from usuario_roles ur join usuarios u on u.id=ur.usuario_id where u.username='supervisor'")) {
                assertTrue(rows.next());
                assertEquals(2, rows.getInt(1));
            }
        }
    }
}
