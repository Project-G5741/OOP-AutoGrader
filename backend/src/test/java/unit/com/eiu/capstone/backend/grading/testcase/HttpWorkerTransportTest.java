package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;

import com.eiu.capstone.backend.grading.testcase.WorkerSpawnException;
import com.eiu.capstone.backend.grading.testcase.WorkerTimeoutException;
import com.eiu.capstone.backend.grading.testcase.transport.HttpWorkerTransport;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpWorkerTransportTest {

    private HttpServer server;
    private String baseUrl;
    private String lastInvokeBody;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/sessions/s1/invoke", exchange -> {
            lastInvokeBody = new String(exchange.getRequestBody().readAllBytes());
            byte[] response = "{\"kind\":\"NORMAL\",\"returnValue\":7}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/sessions/s1", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void invokeRoundTripForwardsNdjsonLine() {
        HttpWorkerTransport transport = new HttpWorkerTransport(
                HttpClient.newHttpClient(), baseUrl, "s1", "token");
        transport.writeLine("{\"op\":\"invoke\"}");
        String line = transport.readLine(Duration.ofSeconds(5), 65536);
        assertEquals("{\"kind\":\"NORMAL\",\"returnValue\":7}", line);
        assertEquals("{\"op\":\"invoke\"}", lastInvokeBody);
        transport.closeTransport();
    }

    @Test
    void invokeTimeoutMapsToWorkerTimeoutException() {
        server.createContext("/sessions/s2/invoke", exchange -> {
            byte[] response = "".getBytes();
            exchange.sendResponseHeaders(504, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        HttpWorkerTransport transport = new HttpWorkerTransport(
                HttpClient.newHttpClient(), baseUrl, "s2", "token");
        transport.writeLine("{\"op\":\"invoke\"}");
        assertThrows(WorkerTimeoutException.class,
                () -> transport.readLine(Duration.ofSeconds(5), 65536));
    }

    @Test
    void invokeFailureMapsToWorkerSpawnException() {
        server.createContext("/sessions/s3/invoke", exchange -> {
            byte[] response = "bad".getBytes();
            exchange.sendResponseHeaders(503, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        HttpWorkerTransport transport = new HttpWorkerTransport(
                HttpClient.newHttpClient(), baseUrl, "s3", "token");
        transport.writeLine("{\"op\":\"invoke\"}");
        assertThrows(WorkerSpawnException.class,
                () -> transport.readLine(Duration.ofSeconds(5), 65536));
    }
}
