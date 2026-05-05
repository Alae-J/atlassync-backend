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

class WhatsAppDeliveryChannelTest {

    private static final String SUCCESS_BODY = """
            {
              "messaging_product": "whatsapp",
              "contacts":  [ { "input": "212600000000", "wa_id": "212600000000" } ],
              "messages":  [ { "id": "wamid.HBgM..." } ]
            }
            """;

    private MockRestServiceServer server;
    private WhatsAppDeliveryChannel channel;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var props = new WhatsAppProperties(
                "https://example.test/v20.0",
                "123456789",
                "EAA-token",
                "atlassync_otp",
                "en"
        );
        channel = new WhatsAppDeliveryChannel(builder, props);
    }

    @Test
    void postsTemplatePayloadToCloudApi() {
        server.expect(requestTo("https://example.test/v20.0/123456789/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer EAA-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("212600000000"))
                .andExpect(jsonPath("$.type").value("template"))
                .andExpect(jsonPath("$.template.name").value("atlassync_otp"))
                .andExpect(jsonPath("$.template.language.code").value("en"))
                .andExpect(jsonPath("$.template.components[0].type").value("body"))
                .andExpect(jsonPath("$.template.components[0].parameters[0].text").value("482910"))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        channel.deliver(new OtpDelivery("+212600000000", "482910", "ignored for templates"));
        server.verify();
    }

    @Test
    void wrapsApiFailuresInDeliveryException() {
        server.expect(requestTo("https://example.test/v20.0/123456789/messages"))
                .andRespond(withServerError());

        assertThatThrownBy(() ->
                channel.deliver(new OtpDelivery("+212600000001", "111111", "boom")))
                .isInstanceOf(DeliveryException.class);
        server.verify();
    }

    @Test
    void rejectsBlankCredentialsAtConstruction() {
        assertThatThrownBy(() -> new WhatsAppProperties(null, "", "tok", "tpl", "en"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("phone-number-id");

        assertThatThrownBy(() -> new WhatsAppProperties(null, "123", null, "tpl", "en"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("access-token");

        assertThatThrownBy(() -> new WhatsAppProperties(null, "123", "tok", "", "en"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("template-name");
    }

    @Test
    void usesDefaultBaseUrlAndLanguage() {
        var props = new WhatsAppProperties(null, "123", "tok", "tpl", null);
        assertThat(props.baseUrl()).isEqualTo("https://graph.facebook.com/v20.0");
        assertThat(props.languageCode()).isEqualTo("en");
    }
}
