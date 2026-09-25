package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import com.pulsewallet.pulsewallet.dto.FraudCheckRequest;
import com.pulsewallet.pulsewallet.dto.FraudCheckResponse;

class FraudDetectionServiceTest {

    private static final String FRAUD_API_URL = "http://fake-fraud-api:9999";

    private MockRestServiceServer mockServer;
    private FraudDetectionService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new FraudDetectionService(builder, FRAUD_API_URL, 2);
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
    void checkFraud_returnsSafeDefaultWhenFastApiIsUnavailable() {
        mockServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andRespond(MockRestResponseCreators.withServerError());
        mockServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andRespond(MockRestResponseCreators.withServerError());
        mockServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andRespond(MockRestResponseCreators.withServerError());

        FraudCheckResponse response = service.checkFraud(sampleRequest());

        assertThat(response.isFraud()).isFalse();
        assertThat(response.fraudProbability()).isZero();
        assertThat(response.riskScore()).isZero();
        assertThat(response.riskLevel()).isEqualTo("LOW");
        mockServer.verify();
    }

    @Test
    void checkFraud_retriesTransientFailureBeforeReturningSuccess() {
        String jsonResponse = """
                {
                    "is_fraud": true,
                    "fraud_probability": 0.81,
                    "risk_score": 81.0,
                    "risk_level": "HIGH"
                }
                """;

        RestClient.Builder retryBuilder = RestClient.builder();
        MockRestServiceServer retryServer = MockRestServiceServer.bindTo(retryBuilder).build();
        retryServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andRespond(MockRestResponseCreators.withServerError());
        retryServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        FraudDetectionService retryService = new FraudDetectionService(retryBuilder, FRAUD_API_URL, 1);

        FraudCheckResponse response = retryService.checkFraud(sampleRequest());

        assertThat(response.isFraud()).isTrue();
        assertThat(response.fraudProbability()).isEqualTo(0.81);
        assertThat(response.riskScore()).isEqualTo(81.0);
        assertThat(response.riskLevel()).isEqualTo("HIGH");
        retryServer.verify();
    }

    @Test
    void checkFraud_returnsSafeDefaultAfterRetryLimitIsExceeded() {
        RestClient.Builder retryBuilder = RestClient.builder();
        MockRestServiceServer retryServer = MockRestServiceServer.bindTo(retryBuilder).build();
        retryServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andRespond(MockRestResponseCreators.withServerError());
        retryServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andRespond(MockRestResponseCreators.withServerError());

        FraudDetectionService retryService = new FraudDetectionService(retryBuilder, FRAUD_API_URL, 1);

        FraudCheckResponse response = retryService.checkFraud(sampleRequest());

        assertThat(response.isFraud()).isFalse();
        assertThat(response.fraudProbability()).isZero();
        assertThat(response.riskScore()).isZero();
        assertThat(response.riskLevel()).isEqualTo("LOW");
        retryServer.verify();
    }

    @Test
    void checkFraud_doesNotRetryNonTransientClientError() {
        mockServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andRespond(MockRestResponseCreators.withBadRequest());

        FraudCheckResponse response = service.checkFraud(sampleRequest());

        assertThat(response.isFraud()).isFalse();
        assertThat(response.fraudProbability()).isZero();
        assertThat(response.riskScore()).isZero();
        assertThat(response.riskLevel()).isEqualTo("LOW");
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

        mockServer.expect(MockRestRequestMatchers.requestTo(FRAUD_API_URL + "/fraud-check"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        service.checkFraud(sampleRequest());

        mockServer.verify();
    }

    private FraudCheckRequest sampleRequest() {
        return new FraudCheckRequest(
                0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                149.62);
    }
}
