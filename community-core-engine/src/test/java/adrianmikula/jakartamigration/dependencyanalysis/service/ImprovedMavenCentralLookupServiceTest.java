package adrianmikula.jakartamigration.dependencyanalysis.service;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImprovedMavenCentralLookupServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void packageRenameSearchFindsJakartaEquivalentForUnknownLibrary() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);

        HttpResponse<String> emptyResponse = mock(HttpResponse.class);
        when(emptyResponse.statusCode()).thenReturn(200);
        when(emptyResponse.body()).thenReturn("{\"response\":{\"numFound\":0,\"docs\":[]}}");

        HttpResponse<String> jakartaXmlRpcResponse = mock(HttpResponse.class);
        when(jakartaXmlRpcResponse.statusCode()).thenReturn(200);
        when(jakartaXmlRpcResponse.body()).thenReturn(
            "{\"response\":{\"numFound\":1,\"docs\":[{\"g\":\"jakarta.xml.rpc\",\"a\":\"jakarta.xml.rpc-api\",\"latestVersion\":\"1.9.0\"}]}}");

        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String url = request.uri().toString();
            return url.contains("jakarta.xml.rpc") ? jakartaXmlRpcResponse : emptyResponse;
        });

        Map<String, String> packageRenameMap = Map.of("javax.xml.rpc", "jakarta.xml.rpc");
        ImprovedMavenCentralLookupService service = new ImprovedMavenCentralLookupService(httpClient, packageRenameMap);

        var matches = service.findJakartaEquivalents("org.apache.axis", "axis", Set.of("javax.xml.rpc")).get();

        assertThat(matches).isNotEmpty();
        assertThat(matches.get(0).groupId()).isEqualTo("jakarta.xml.rpc");
        assertThat(matches.get(0).artifactId()).isEqualTo("jakarta.xml.rpc-api");
    }
}
