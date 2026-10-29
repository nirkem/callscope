FROM python:3.12-slim

WORKDIR /srv

# Dependencies first: this layer is rebuilt only when pyproject.toml changes, not on every code edit.
# Day 2: change "." to ".[ml]" for the real models. The CPU index keeps torch small (no CUDA).
COPY pyproject.toml ./
RUN pip install --no-cache-dir --extra-index-url https://download.pytorch.org/whl/cpu "."

COPY app ./app

# Run as a non-root user.
RUN useradd --create-home appuser
USER appuser

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
