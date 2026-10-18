FROM postgres:18-bookworm

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       awscli ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY backup.sh /usr/local/bin/backup.sh

# Normalize Windows CRLF to Unix LF
RUN sed -i 's/\r$//' /usr/local/bin/backup.sh \
    && chmod 0555 /usr/local/bin/backup.sh

USER postgres

ENTRYPOINT ["/usr/local/bin/backup.sh"]