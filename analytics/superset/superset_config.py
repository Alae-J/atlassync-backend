"""
Superset config overrides for the AtlasSync dev stack.
"""
import os

SECRET_KEY = os.getenv("SUPERSET_SECRET_KEY", "atlassync-dev-superset-secret")

# Tighter cache for dashboards — we want recent gold data, not 5-min stale.
DATA_CACHE_CONFIG = {
    "CACHE_TYPE": "SimpleCache",
    "CACHE_DEFAULT_TIMEOUT": 60,
}

# Default Superset metadata DB is SQLite at /app/superset_home/superset.db.
# Kept here for clarity — persisted via a docker volume mount.
SQLALCHEMY_DATABASE_URI = "sqlite:////app/superset_home/superset.db"

# Trino's executemany path uses a dialect feature SQLAlchemy 2 flags as
# deprecated; silence to keep logs readable.
SQLALCHEMY_TRACK_MODIFICATIONS = False
