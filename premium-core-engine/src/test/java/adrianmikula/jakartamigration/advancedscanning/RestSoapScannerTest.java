package adrianmikula.jakartamigration.advancedscanning;

import adrianmikula.jakartamigration.advancedscanning.domain.RestSoapProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.RestSoapScanner;
import adrianmikula.jakartamigration.advancedscanning.service.impl.RestSoapScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for REST/SOAP Scanner
 */
class RestSoapScannerTest {

    private RestSoapScanner scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new RestSoapScannerImpl();
    }

    @Test
    void shouldReturnEmptyResultForEmptyProject() {
        RestSoapProjectScanResult result = scanner.scanProject(tempDir);
        
        assertThat(result.totalFilesScanned()).isEqualTo(0);
        assertThat(result.hasJavaxUsage()).isFalse();
    }

    @Test
    void shouldDetectJavaxWsRsImports() throws Exception {
        Path testFile = tempDir.resolve("RestService.java");
        String content = """
            package com.example;
            
            import javax.ws.rs.GET;
            import javax.ws.rs.POST;
            import javax.ws.rs.Path;
            import javax.ws.rs.PathParam;
            import javax.ws.rs.QueryParam;
            import javax.ws.rs.core.Response;
            
            @Path("/api")
            public class RestService {
                @GET
                @Path("/{id}")
                public Response get(@PathParam("id") String id) { return null; }
            }
            """;
        Files.writeString(testFile, content);

        RestSoapProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result.hasJavaxUsage()).isTrue();
    }

    @Test
    void shouldDetectSoapImports() throws Exception {
        Path testFile = tempDir.resolve("SoapService.java");
        String content = """
            package com.example;
            
            import javax.xml.ws.WebService;
            import javax.xml.ws.soap.SOAPBinding;
            import javax.jws.WebMethod;
            import javax.jws.WebParam;
            
            @WebService
            @SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
            public class SoapService {
                @WebMethod
                public String hello(@WebParam(name = "name") String name) { return null; }
            }
            """;
        Files.writeString(testFile, content);

        RestSoapProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result.hasJavaxUsage()).isTrue();
    }
}
