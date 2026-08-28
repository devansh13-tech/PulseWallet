package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.pulsewallet.pulsewallet.dto.FraudCheckRequest;
import com.pulsewallet.pulsewallet.dto.FraudCheckResponse;

/**
 * Unit tests for {@link FraudDetectionService}.
 *
 * <p>Uses {@link MockRestServiceServer} bound to the same
 * {@link RestClient.Builder} the service uses, so outgoing HTTP calls are
 * intercepted without a real network connection.
 */
class FraudDetectionServiceTest {

    private static final String FRAUD_API_URL = "http://fake-fraud-api:9999";

    private MockRestServiceServer mockServer;
    private FraudDetectionService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new FraudDetectionService(builder, FRAUD_API_URL);
    }

    @Test
    void checkFraud_returnsDeserializedResponseOnSuccess() {
        String jsonResponse = """
                {
                    "is_fraud": true,
                    "fraud_probability": 0.92,
                    "risk_score": 92.0,
                    "risk_level": "CRITICAL"
                }
                """;
        mockServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        FraudCheckResponse response = service.checkFraud(sampleRequest());

        assertThat(response.isFraud()).isTrue();
        assertThat(response.fraudProbability()).isEqualTo(0.92);
        assertThat(response.riskScore()).isEqualTo(92.0);
        assertThat(response.riskLevel()).isEqualTo("CRITICAL");
        mockServer.verify();
    }

    @Test
    void checkFraud_returnsLowRiskResponseCorrectly() {
        String jsonResponse = """
                {
                    "is_fraud": false,
                    "fraud_probability": 0.05,
                    "risk_score": 5.0,
                    "risk_level": "LOW"
                }
                """;
        mockServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        FraudCheckResponse response = service.checkFraud(sampleRequest());

        assertThat(response.isFraud()).isFalse();
        assertThat(response.fraudProbability()).isEqualTo(0.05);
        assertThat(response.riskScore()).isEqualTo(5.0);
        assertThat(response.riskLevel()).isEqualTo("LOW");
        mockServer.verify();
    }

    @Test
    void checkFraud_propagatesExceptionWhenFastApiIsUnavailable() {
        mockServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> service.checkFraud(sampleRequest()))
                .isInstanceOf(RestClientException.class);
        mockServer.verify();
    }

    @Test
    void checkFraud_sendsRequestToCorrectUrl() {
        String jsonResponse = """
                {
                    "is_fraud": false,
                    "fraud_probability": 0.01,
                    "risk_score": 1.0,
                    "risk_level": "LOW"
                }
                """;
        // Verifies the full URL = baseUrl + "/fraud-check"
        mockServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        service.checkFraud(sampleRequest());

        mockServer.verify();
    }

    /** Builds a minimal valid request — actual feature values don't matter here. */
    private FraudCheckRequest sampleRequest() {
        return new FraudCheckRequest(
                0.0,                          // Time
                -1.3, -0.07, 2.5, 4.0,       // V1–V4
                0.5, -1.2, -0.8, 0.2, -0.5,  // V5–V9
                0.0, -2.1, 4.2, -2.3, -1.0,  // V10–V14
                -0.1, 0.0, -0.5, -0.2, 0.1,  // V15–V19
                0.0, 0.2, 0.7, -0.1, -0.3,   // V20–V24
                0.0, 0.3, -0.1, 0.0,         // V25–V28
                149.62                         // Amount
        );
    }
}
