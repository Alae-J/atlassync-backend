"""
analytics-api — thin HTTP layer over the gold Delta tables on HDFS.

Reads written by Spark jobs; this service treats them as a read-only catalog.
The real per-user endpoints land in #20. For now this is the smoke-test scaffold:
- /health for liveness
- /_debug/gold/monthly-spend dumps the whole gold table, used to verify HDFS
  connectivity end-to-end during #19.
"""
import json
import os

from deltalake import DeltaTable
from fastapi import FastAPI, HTTPException

GOLD_MONTHLY_SPEND_PATH = os.getenv(
    "GOLD_MONTHLY_SPEND_PATH",
    "hdfs://hadoop-namenode:9000/atlassync/gold/user_monthly_spend",
)

app = FastAPI(title="atlassync-analytics-api", version="0.1.0")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/_debug/gold/monthly-spend")
def gold_sample():
    """Dump the whole gold/user_monthly_spend Delta table. Will be replaced
    by the per-user endpoint in #20."""
    try:
        dt = DeltaTable(GOLD_MONTHLY_SPEND_PATH)
        df = dt.to_pandas()
        # pandas.to_json handles Decimal/datetime/date natively.
        return json.loads(df.to_json(orient="records", date_format="iso"))
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"delta read failed: {type(e).__name__}: {e}",
        )
