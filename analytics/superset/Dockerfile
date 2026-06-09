FROM apache/superset:4.1.1

USER root
# The Trino SQLAlchemy dialect isn't in the default Superset image — without
# it Superset can't talk to Trino. The official `trino` PyPI package ships
# both the client and the dialect.
RUN pip install --no-cache-dir trino==0.330.0
USER superset
