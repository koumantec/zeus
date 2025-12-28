FROM python:3.11-slim

WORKDIR /app

COPY pyproject.toml /app/
RUN pip install --no-cache-dir -U pip && \
    pip install --no-cache-dir .

COPY app /app/app
COPY tests /app/tests

EXPOSE 8000
CMD ["python", "-c", "print('Backend skeleton ready')"]
