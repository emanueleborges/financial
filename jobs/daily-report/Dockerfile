FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
# pip >= 26.2.2 e certifi >= 2026.10.19 (requirements.txt): incidente eMudhra/emSign
RUN pip install --no-cache-dir --upgrade "pip>=26.2.2" \
    && pip install --no-cache-dir -r requirements.txt
COPY report.py .
CMD ["python", "-u", "report.py"]
