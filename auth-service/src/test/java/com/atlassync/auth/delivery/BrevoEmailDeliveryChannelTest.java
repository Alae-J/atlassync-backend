package com.atlassync.auth.delivery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrevoEmailDeliveryChannelTest {

    private static final String SUCCESS_BODY = """
            { "messageId": "<abc-123@brevo>" }
            """;

    private MockRestServiceServer server;
    private BrevoEmailDeliveryChannel channel;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var props = new BrevoProperties(
                "https://example.test",
                "xkeysib-test",
                "noreply@atlassync.local",
                "AtlasSync",
                "Your AtlasSync verification code"
        );
        channel = new BrevoEmailDeliveryChannel(builder, props);
    }

    @Test
    void postsTransactionalEmailWithApiKeyHeader() {
        server.expect(requestTo("https://example.test/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "xkeysib-test"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.name").value("AtlasSync"))
                .andExpect(jsonPath("$.sender.email").value("noreply@atlassync.local"))
                .andExpect(jsonPath("$.to[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.subject").value("Your AtlasSync verification code"))
                .andExpect(jsonPath("$.textContent").value(org.hamcrest.Matchers.containsString("482910")))
                .andExpect(jsonPath("$.htmlContent").value(org.hamcrest.Matchers.containsString("482910")))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        channel.deliver(new OtpDelivery(
                "user@example.com",
                "482910",
                "Your AtlasSync code is 482910. Expires in 5 minutes."
        ));
        server.verify();
    }

    @Test
    void wrapsApiFailuresInDeliveryException() {
        server.expect(requestTo("https://example.test/v3/smtp/email"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> channel.deliver(new OtpDelivery(
                "user@example.com", "111111", "boom")))
                .isInstanceOf(DeliveryException.class);
        server.verify();
    }

    @Test
    void rejectsBlankApiKeyOrFromEmail() {
        assertThatThrownBy(() -> new BrevoProperties(null, "", "noreply@x.com", "n", "s"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api-key");

        assertThatThrownBy(() -> new BrevoProperties(null, "key", null, "n", "s"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("from-email");
    }

    @Test
    void appliesSensibleDefaults() {
        var props = new BrevoProperties(null, "k", "n@a.com", null, null);
        assertThat(props.baseUrl()).isEqualTo("https://api.brevo.com");
        assertThat(props.fromName()).isEqualTo("AtlasSync");
        assertThat(props.subject()).isEqualTo("Your AtlasSync verification code");
    }
}
