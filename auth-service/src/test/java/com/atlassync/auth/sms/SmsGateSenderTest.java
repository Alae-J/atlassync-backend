package com.atlassync.auth.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmsGateSenderTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private SmsGateSender sender;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var props = new SmsGateProperties("https://example.test/v1", "user", "pass");
        sender = new SmsGateSender(builder, props);
    }

    @Test
    void postsBasicAuthAndPayloadToMessagesEndpoint() {
        server.expect(requestTo("https://example.test/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        { "message": "code 123456", "phoneNumbers": ["+212600000000"] }
                        """))
                .andRespond(withSuccess("{\"id\":\"abc-123\",\"state\":\"Pending\"}",
                        MediaType.APPLICATION_JSON));

        sender.send("+212600000000", "code 123456");
        server.verify();
    }

    @Test
    void wrapsRemoteFailuresInSmsDeliveryException() {
        server.expect(requestTo("https://example.test/v1/messages"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> sender.send("+212600000001", "boom"))
                .isInstanceOf(SmsDeliveryException.class);
        server.verify();
    }

    @Test
    void rejectsBlankCredentialsAtConstruction() {
        assertThatThrownBy(() -> new SmsGateProperties(null, "", "pass"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("username");

        assertThatThrownBy(() -> new SmsGateProperties(null, "user", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("password");
    }

    @Test
    void usesPublicGatewayUrlWhenBaseUrlOmitted() {
        var props = new SmsGateProperties(null, "u", "p");
        org.assertj.core.api.Assertions.assertThat(props.baseUrl())
                .isEqualTo("https://api.sms-gate.app/3rdparty/v1");
    }
}
