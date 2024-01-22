FROM python:3.10-slim

# Install Graphviz and Python
RUN apt-get update \
  && apt-get install -y --no-install-recommends graphviz python3 python3-pip \
  && rm -rf /var/lib/apt/lists/*

# Install Flask
RUN pip3 install flask

# Copy the Python script into the container
COPY ./graphConverter.py /app.py

# Expose the port the app runs on
EXPOSE 5000

# Run the app
CMD ["python3", "/app.py"]
