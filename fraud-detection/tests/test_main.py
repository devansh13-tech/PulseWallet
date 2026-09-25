import os
import unittest
from unittest.mock import patch

from api import main


class StubScaler:
    def transform(self, values):
        return values


class StubModel:
    def __init__(self, probability):
        self.probability = probability

    def predict_proba(self, values):
        return [[1.0 - self.probability, self.probability]]


def sample_request():
    return main.FraudCheckRequest(
        Time=0.0,
        V1=0.0,
        V2=0.0,
        V3=0.0,
        V4=0.0,
        V5=0.0,
        V6=0.0,
        V7=0.0,
        V8=0.0,
        V9=0.0,
        V10=0.0,
        V11=0.0,
        V12=0.0,
        V13=0.0,
        V14=0.0,
        V15=0.0,
        V16=0.0,
        V17=0.0,
        V18=0.0,
        V19=0.0,
        V20=0.0,
        V21=0.0,
        V22=0.0,
        V23=0.0,
        V24=0.0,
        V25=0.0,
        V26=0.0,
        V27=0.0,
        V28=0.0,
        Amount=10.0,
    )


class FraudThresholdTest(unittest.TestCase):

    def test_fraud_check_uses_configured_threshold(self):
        with patch.object(main, "scaler", StubScaler()), patch.object(main, "FRAUD_THRESHOLD", 0.5):
            for probability, expected_is_fraud in ((0.49, False), (0.5, True), (0.51, True)):
                with self.subTest(probability=probability):
                    with patch.object(main, "model", StubModel(probability)):
                        response = main.fraud_check(sample_request())

                    self.assertIs(response["is_fraud"], expected_is_fraud)
                    self.assertEqual(
                        set(response),
                        {"is_fraud", "fraud_probability", "risk_score", "risk_level"},
                    )

    def test_load_fraud_threshold_defaults_to_half(self):
        with patch.dict(os.environ, {}, clear=False):
            os.environ.pop("FRAUD_SCORE_THRESHOLD", None)
            self.assertEqual(main.load_fraud_threshold(), 0.5)

    def test_load_fraud_threshold_rejects_invalid_values(self):
        for value in ("-0.01", "1.01", "not-a-number"):
            with self.subTest(value=value), patch.dict(
                os.environ, {"FRAUD_SCORE_THRESHOLD": value}
            ):
                with self.assertRaisesRegex(ValueError, "FRAUD_SCORE_THRESHOLD"):
                    main.load_fraud_threshold()
