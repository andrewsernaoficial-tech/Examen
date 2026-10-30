package pe.edu.upeu.vetandes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VentasIntegrationTest {
    @LocalServerPort int port;
    final HttpClient client = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();
    long clienteId, categoriaId, productoId;
    String token;
    record Respuesta(int estado, JsonNode body) {}
    Respuesta call(String method, String path, String body) throws Exception {
        var req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/" + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + (token == null ? "" : token))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString());
        return new Respuesta(res.statusCode(), res.body().isEmpty() ? null : json.readTree(res.body()));
    }
    @BeforeEach void preparar() throws Exception {
        token = null;
        var login = call("POST", "auth/login", "{\"username\":\"admin\",\"password\":\"Admin2026*\"}");
        assertEquals(200, login.estado());
        token = login.body().get("token").asText();
        String uid = UUID.randomUUID().toString();
        var c = call("POST", "categorias", "{\"nombre\":\"Cat " + uid + "\",\"estado\":true}");
        assertEquals(201, c.estado()); categoriaId = c.body().get("id").asLong();
        String dni = String.format("%08d", ThreadLocalRandom.current().nextInt(10000000,99999999));
        var cli = call("POST", "clientes", "{\"dni\":\""+dni+"\",\"nombres\":\"Ana\",\"apellidos\":\"Huamán\",\"email\":\""+uid+"@example.com\",\"estado\":true}");
        assertEquals(201, cli.estado()); clienteId = cli.body().get("id").asLong();
        productoId = producto("Producto " + uid, "12.50", 10);
    }
    long producto(String nombre, String precio, int stock) throws Exception {
        var r = call("POST", "productos", "{\"nombre\":\""+nombre+"\",\"precio\":"+precio+",\"stock\":"+stock+",\"estado\":true,\"categoria\":"+categoriaId+"}");
        assertEquals(201, r.estado()); return r.body().get("id").asLong();
    }
    String venta(long producto, int cantidad) {
        return "{\"clienteId\":"+clienteId+",\"detalles\":[{\"productoId\":"+producto+",\"cantidad\":"+cantidad+"}]}";
    }
    int stock(long id) throws Exception { return call("GET","productos/"+id,null).body().get("stock").asInt(); }
    @Test void calculaEnServidorDescuentaYConsultaPagina() throws Exception {
        var r = call("POST", "ventas", venta(productoId,3));
        assertEquals(201,r.estado()); assertEquals(37.50,r.body().get("total").asDouble());
        assertEquals(7,stock(productoId));
        var buscar=call("GET","ventas/buscar?clienteId="+clienteId+"&estado=REGISTRADA&pagina=0&tamanio=1",null);
        assertEquals(200,buscar.estado()); assertEquals(1,buscar.body().get("totalElementos").asInt());
        assertEquals(1,buscar.body().get("contenido").size());
        var detalle=call("GET","ventas/"+r.body().get("id").asLong(),null);
        assertEquals(37.50,detalle.body().get("detalles").get(0).get("subtotal").asDouble());
    }
    @Test void rollbackRestauraLineasAnteriores() throws Exception {
        long sinStock=producto("Sin stock "+UUID.randomUUID(),"3.00",0);
        String body="{\"clienteId\":"+clienteId+",\"detalles\":[{\"productoId\":"+productoId+",\"cantidad\":2},{\"productoId\":"+sinStock+",\"cantidad\":1}]}";
        assertEquals(409,call("POST","ventas",body).estado());
        assertEquals(10,stock(productoId));
        assertEquals(0,call("GET","ventas/buscar?clienteId="+clienteId,null).body().get("totalElementos").asInt());
    }
    @Test void dosCajasNoVendenLasMismasUltimasUnidades() throws Exception {
        long ultimo=producto("Ultimo "+UUID.randomUUID(),"5.00",1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var barrera=new CyclicBarrier(2);
            Callable<Integer> vender=()->{barrera.await();return call("POST","ventas",venta(ultimo,1)).estado();};
            var a=pool.submit(vender);var b=pool.submit(vender);
            var estados=new java.util.ArrayList<>(java.util.List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS)));
            java.util.Collections.sort(estados);assertEquals(java.util.List.of(201,409),estados);
        }
        assertEquals(0,stock(ultimo));
        assertEquals(1,call("GET","ventas/buscar?clienteId="+clienteId,null).body().get("totalElementos").asInt());
    }
    @Test void anularDevuelveStockUnaSolaVezYReporteExcluyeAnuladas() throws Exception {
        var r=call("POST","ventas",venta(productoId,2));long id=r.body().get("id").asLong();
        assertEquals(200,call("PATCH","ventas/"+id+"/anular",null).estado());
        assertEquals(10,stock(productoId));
        assertEquals(409,call("PATCH","ventas/"+id+"/anular",null).estado());
        assertEquals(10,stock(productoId));
        var report=call("GET","reportes/ventas-por-categoria",null);assertEquals(200,report.estado());
        for(var row:report.body())assertNotEquals(categoriaId,row.get("categoriaId").asLong());
    }
    @Test void validaCabeceraDetalleRangoYProductoInexistente() throws Exception {
        assertEquals(400,call("POST","ventas","{\"detalles\":[]}").estado());
        assertEquals(400,call("POST","ventas",venta(productoId,0)).estado());
        assertEquals(404,call("POST","ventas",venta(99999999,1)).estado());
        assertEquals(409,call("GET","ventas/buscar?desde=2026-10-10&hasta=2026-10-01",null).estado());
        assertEquals(409,call("GET","ventas/buscar?tamanio=0",null).estado());
    }
    @Test void paginaSinPerderVentasYReporteSumaCategorias() throws Exception {
        for(int i=0;i<3;i++)assertEquals(201,call("POST","ventas",venta(productoId,1)).estado());
        var p=call("GET","ventas/buscar?clienteId="+clienteId+"&pagina=1&tamanio=2",null).body();
        assertEquals(3,p.get("totalElementos").asInt());assertEquals(2,p.get("totalPaginas").asInt());
        assertEquals(1,p.get("contenido").size());assertTrue(p.get("ultima").asBoolean());
        var report=call("GET","reportes/ventas-por-categoria",null).body();boolean encontrado=false;
        for(var row:report)if(row.get("categoriaId").asLong()==categoriaId){encontrado=true;assertEquals(3,row.get("cantidadTotal").asInt());assertEquals(37.50,row.get("montoTotal").asDouble());}
        assertTrue(encontrado);
    }
    @Test void rechazaCantidadDecimalSinDescontarStock() throws Exception {
        String body="{\"clienteId\":"+clienteId+",\"detalles\":[{\"productoId\":"+productoId+",\"cantidad\":2.5}]}";
        assertEquals(400,call("POST","ventas",body).estado());
        assertEquals(10,stock(productoId));
    }
}
