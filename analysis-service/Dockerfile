FROM python:3.12-slim

WORKDIR /srv

# Dependencies first: this layer is rebuilt only when pyproject.toml changes, not on every code edit.
# ".[ml]" adds transformers and torch. The CPU index gives the CPU-only torch build (no CUDA, much smaller).
COPY pyproject.toml ./
RUN pip install --no-cache-dir --extra-index-url https://download.pytorch.org/whl/cpu ".[ml]"

COPY app ./app

# Run as a non-root user.
RUN useradd --create-home appuser
USER appuser

# Where Hugging Face keeps downloaded models. docker-compose mounts a volume here, so the models
# are downloaded on the first start only. Created as appuser so the volume is writable.
ENV HF_HOME=/home/appuser/.cache/huggingface
RUN mkdir -p "$HF_HOME"

# The image runs the real models; tests and local runs default to the fakes.
ENV CALLSCOPE_MODELS=real

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
