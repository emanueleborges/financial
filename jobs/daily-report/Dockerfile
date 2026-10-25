FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
# pip 26.2.2: o certifi embutido no pip deixa de confiar nas CAs eMudhra/emSign comprometidas
RUN pip install --no-cache-dir --only-binary :all: "pip==26.2.2"
RUN pip install --no-cache-dir -r requirements.txt
COPY report.py .
CMD ["python", "-u", "report.py"]
