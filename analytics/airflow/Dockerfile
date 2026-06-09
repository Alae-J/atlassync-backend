FROM apache/airflow:2.10.4

USER root
# Install the Docker CLI so BashOperator tasks can `docker exec` into the
# spark-master / kafka / hadoop containers. Use Docker's official apt repo
# instead of Debian's `docker.io` package — the latter ships an older
# client (1.41) that gets rejected by modern Docker daemons that require
# API >= 1.44.
# Create a `hostdocker` group matching the host's docker GID (126 on this
# dev machine) so the airflow user can access the mounted docker.sock.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg \
        | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
    && chmod a+r /etc/apt/keyrings/docker.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
        > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 126 hostdocker \
    && usermod -aG hostdocker airflow

USER airflow
