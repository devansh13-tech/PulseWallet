"""
Standalone Model Training Pipeline for PulseWallet Fraud Detection Engine.
Applies StandardScaler, SMOTE oversampling on training split, trains XGBoost,
evaluates performance metrics, and exports serialized production model artifacts.
"""

import argparse
import os
from pathlib import Path
import joblib
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, precision_recall_curve, roc_auc_score, auc
from imblearn.over_sampling import SMOTE
import xgboost as xgb

from config.features import FEATURE_NAMES
from src.preprocessing.pipeline import TransactionPreprocessor


def train_model(
    data_path: str,
    models_dir: str,
    random_state: int = 42,
    test_size: float = 0.2,
) -> dict:
    """
    Trains XGBoost fraud detection model with SMOTE and exports artifacts.
    """
    models_path = Path(models_dir)
    models_path.mkdir(parents=True, exist_ok=True)

    csv_path = Path(data_path)
    if not csv_path.exists():
        print(f"[WARN] Data file not found at {csv_path}. Creating synthetic stratified dataset for validation.")
        n_samples = 5000
        n_fraud = 50
        rng = np.random.RandomState(random_state)
        
        # Normal transactions
        normal_data = rng.randn(n_samples - n_fraud, 30)
        normal_data[:, 0] = rng.uniform(0, 172800, n_samples - n_fraud) # Time
        normal_data[:, 29] = rng.exponential(80, n_samples - n_fraud)    # Amount
        
        # Fraud transactions (anomalous V1-V28, higher amount, unusual time)
        fraud_data = rng.randn(n_fraud, 30) * 2.5
        fraud_data[:, 0] = rng.uniform(0, 172800, n_fraud)
        fraud_data[:, 29] = rng.exponential(350, n_fraud)
        
        X_all = np.vstack([normal_data, fraud_data])
        y_all = np.array([0] * (n_samples - n_fraud) + [1] * n_fraud)
        
        df = pd.DataFrame(X_all, columns=FEATURE_NAMES)
        df["Class"] = y_all
    else:
        print(f"[INFO] Loading dataset from {csv_path}...")
        df = pd.read_csv(csv_path)

    X = df[FEATURE_NAMES]
    y = df["Class"]

    # Stratified train-test split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=test_size, random_state=random_state, stratify=y
    )

    # Fit preprocessor on training set only (prevents data leakage)
    preprocessor = TransactionPreprocessor()
    preprocessor.fit(X_train)
    X_train_scaled = preprocessor.transform(X_train)
    X_test_scaled = preprocessor.transform(X_test)

    # Apply SMOTE only to training set
    print("[INFO] Applying SMOTE oversampling on training partition...")
    smote = SMOTE(random_state=random_state)
    X_train_res, y_train_res = smote.fit_resample(X_train_scaled, y_train)

    # Train XGBoost
    print("[INFO] Training XGBoost classifier...")
    model = xgb.XGBClassifier(
        n_estimators=100,
        max_depth=5,
        learning_rate=0.1,
        subsample=0.8,
        colsample_bytree=0.8,
        eval_metric="logloss",
        random_state=random_state,
        n_jobs=-1,
    )
    model.fit(X_train_res, y_train_res)

    # Evaluate on untouched test set
    y_prob = model.predict_proba(X_test_scaled)[:, 1]
    y_pred = (y_prob >= 0.5).astype(int)

    precision, recall, _ = precision_recall_curve(y_test, y_prob)
    pr_auc = auc(recall, precision)
    roc_auc = roc_auc_score(y_test, y_prob)

    report = classification_report(y_test, y_pred, output_dict=True)
    print("\n--- Evaluation Results ---")
    print(f"ROC-AUC: {roc_auc:.4f}")
    print(f"PR-AUC:  {pr_auc:.4f}")
    print(classification_report(y_test, y_pred))

    # Export serialized artifacts
    scaler_out = models_path / "fraud_scaler.joblib"
    model_out = models_path / "xgboost_fraud_model.joblib"

    preprocessor.save(str(scaler_out))
    joblib.dump(model, str(model_out))

    print(f"[SUCCESS] Saved scaler artifact to: {scaler_out}")
    print(f"[SUCCESS] Saved XGBoost model artifact to: {model_out}")

    return {
        "roc_auc": roc_auc,
        "pr_auc": pr_auc,
        "report": report,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train PulseWallet Fraud Detection Engine")
    parser.add_argument("--data", type=str, default="data/creditcard.csv", help="Path to creditcard.csv")
    parser.add_argument("--models-dir", type=str, default="models", help="Directory to save model artifacts")
    args = parser.parse_args()

    train_model(data_path=args.data, models_dir=args.models_dir)
