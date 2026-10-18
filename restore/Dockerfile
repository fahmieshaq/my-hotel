FROM postgres:18-bookworm

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       awscli ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY restore.sh /usr/local/bin/restore.sh

# Fix Windows CRLF line endings
RUN sed -i 's/\r$//' /usr/local/bin/restore.sh \
    && chmod 0555 /usr/local/bin/restore.sh

USER postgres

ENTRYPOINT ["/usr/local/bin/restore.sh"]
