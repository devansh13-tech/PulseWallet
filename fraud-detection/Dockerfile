# ==============================================================================
# PulseWallet - Fraud Detection Engine (FastAPI + XGBoost)
# ==============================================================================

FROM python:3.11-slim

WORKDIR /app

# Install system dependencies if required for compiling C extensions
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copy and install python dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy application directories and model artifacts
COPY config/ config/
COPY models/ models/
COPY src/ src/
COPY api/ api/

# Create non-root runner user
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

ENV PYTHONUNBUFFERED=1 \
    PORT=8000 \
    FRAUD_SCORE_THRESHOLD=0.5

EXPOSE 8000

# Container Health Check
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8000/health || exit 1

ENTRYPOINT ["uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "8000"]
