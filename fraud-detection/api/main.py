from pathlib import Path
import os

import joblib
import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel

from config.features import FEATURE_NAMES, FEATURE_COUNT



BASE_DIR = Path(__file__).resolve().parent.parent


MODEL_PATH = BASE_DIR / "models" / "xgboost_fraud_model.joblib"
SCALER_PATH = BASE_DIR / "models" / "fraud_scaler.joblib"



model = joblib.load(MODEL_PATH)
scaler = joblib.load(SCALER_PATH)


def load_fraud_threshold() -> float:
    raw_threshold = os.getenv("FRAUD_SCORE_THRESHOLD", "0.5")
    try:
        threshold = float(raw_threshold)
    except ValueError as error:
        raise ValueError("FRAUD_SCORE_THRESHOLD must be a number between 0 and 1") from error

    if not 0.0 <= threshold <= 1.0:
        raise ValueError("FRAUD_SCORE_THRESHOLD must be between 0 and 1")

    return threshold


FRAUD_THRESHOLD = load_fraud_threshold()

class FraudCheckRequest(BaseModel):
    Time: float
    V1: float
    V2: float
    V3: float
    V4: float
    V5: float
    V6: float
    V7: float
    V8: float
    V9: float
    V10: float
    V11: float
    V12: float
    V13: float
    V14: float
    V15: float
    V16: float
    V17: float
    V18: float
    V19: float
    V20: float
    V21: float
    V22: float
    V23: float
    V24: float
    V25: float
    V26: float
    V27: float
    V28: float
    Amount: float


app = FastAPI(
    title="PulseWallet Fraud Detection API",
    description="Machine learning API for transaction fraud detection",
    version="1.0.0",
)


@app.get("/")
def root():
    return {
        "message": "PulseWallet Fraud Detection API is running"
    }


@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "model_loaded": model is not None,
        "scaler_loaded": scaler is not None,
        "feature_count": FEATURE_COUNT,
    }

def calculate_risk_level(risk_score: float) -> str:
    if risk_score < 20:
        return "LOW"
    elif risk_score < 50:
        return "MEDIUM"
    elif risk_score < 75:
        return "HIGH"
    else:
        return "CRITICAL"

    
@app.post("/fraud-check")
def fraud_check(transaction: FraudCheckRequest):
  
    features = [
        transaction.Time,
        transaction.V1,
        transaction.V2,
        transaction.V3,
        transaction.V4,
        transaction.V5,
        transaction.V6,
        transaction.V7,
        transaction.V8,
        transaction.V9,
        transaction.V10,
        transaction.V11,
        transaction.V12,
        transaction.V13,
        transaction.V14,
        transaction.V15,
        transaction.V16,
        transaction.V17,
        transaction.V18,
        transaction.V19,
        transaction.V20,
        transaction.V21,
        transaction.V22,
        transaction.V23,
        transaction.V24,
        transaction.V25,
        transaction.V26,
        transaction.V27,
        transaction.V28,
        transaction.Amount,
    ]

    
    
    scaled_time_amount = scaler.transform(
        [[transaction.Time, transaction.Amount]]
    )[0]

    
    features[0] = scaled_time_amount[0]
    features[29] = scaled_time_amount[1]

   
    model_input = np.array([features])

    
    fraud_probability = model.predict_proba(model_input)[0][1]

    
    is_fraud = fraud_probability >= FRAUD_THRESHOLD

    risk_score = fraud_probability * 100
    risk_level = calculate_risk_level(risk_score)

    return {
        "is_fraud": bool(is_fraud),
        "fraud_probability": float(fraud_probability),
        "risk_score": round(float(risk_score), 2),
        "risk_level": risk_level,
}