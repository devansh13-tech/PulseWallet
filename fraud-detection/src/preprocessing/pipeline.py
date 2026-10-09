"""
Preprocessing pipeline for PulseWallet Fraud Detection Engine.
Standardizes Time and Amount features using StandardScaler while preserving PCA features V1-V28.
"""

from typing import Tuple, Optional
import numpy as np
import pandas as pd
from sklearn.base import BaseEstimator, TransformerMixin
from sklearn.preprocessing import StandardScaler
import joblib

from config.features import FEATURE_NAMES


class TransactionPreprocessor(BaseEstimator, TransformerMixin):
    """
    Standardizes Time and Amount columns and aligns all 30 features.
    Matches the schema expected by the trained XGBoost model.
    """

    def __init__(self):
        self.scaler = StandardScaler()
        self.is_fitted = False

    def fit(self, X: pd.DataFrame | np.ndarray, y: Optional[np.ndarray] = None):
        if isinstance(X, pd.DataFrame):
            time_amount = X[["Time", "Amount"]].values
        else:
            time_amount = X[:, [0, 29]]
        self.scaler.fit(time_amount)
        self.is_fitted = True
        return self

    def transform(self, X: pd.DataFrame | np.ndarray) -> np.ndarray:
        if not self.is_fitted:
            raise RuntimeError("TransactionPreprocessor must be fitted before transforming data.")

        if isinstance(X, pd.DataFrame):
            time_amount = self.scaler.transform(X[["Time", "Amount"]].values)
            df_copy = X[FEATURE_NAMES].copy()
            df_copy["Time"] = time_amount[:, 0]
            df_copy["Amount"] = time_amount[:, 1]
            return df_copy.values
        else:
            X_arr = np.array(X, dtype=float, copy=True)
            time_amount = self.scaler.transform(X_arr[:, [0, 29]])
            X_arr[:, 0] = time_amount[:, 0]
            X_arr[:, 29] = time_amount[:, 1]
            return X_arr

    def save(self, filepath: str) -> None:
        """Serializes the fitted scaler artifact."""
        joblib.dump(self.scaler, filepath)

    @classmethod
    def load(cls, filepath: str) -> "TransactionPreprocessor":
        """Loads a saved scaler artifact into a preprocessor instance."""
        instance = cls()
        instance.scaler = joblib.load(filepath)
        instance.is_fitted = True
        return instance
