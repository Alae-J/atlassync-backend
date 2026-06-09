"""
analytics-api — thin HTTP layer over the gold Delta tables on HDFS.

Reads written by Spark jobs; this service treats them as a read-only catalog.
Auth is handled by the gateway: it validates the JWT and forwards the caller's
user id in the X-User-Id header. This service trusts that header.
"""
import os
from datetime import datetime, timezone
from typing import Annotated

from deltalake import DeltaTable
from deltalake.exceptions import TableNotFoundError
from fastapi import FastAPI, Header, HTTPException

GOLD_MONTHLY_SPEND_PATH = os.getenv(
    "GOLD_MONTHLY_SPEND_PATH",
    "hdfs://hadoop-namenode:9000/atlassync/gold/user_monthly_spend",
)
DEFAULT_CURRENCY = os.getenv("DEFAULT_CURRENCY", "MAD")
HISTORY_MONTHS = 5

app = FastAPI(title="atlassync-analytics-api", version="0.1.0")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/api/analytics/me/monthly-spend")
def monthly_spend(x_user_id: Annotated[int, Header(alias="X-User-Id")]):
    """Current calendar month + the prior {HISTORY_MONTHS} months of spend
    for the caller. Returns zeros for the current month when there is no
    data yet — the mobile UI prefers a zero block over an empty state."""
    today_ym = datetime.now(tz=timezone.utc).strftime("%Y-%m")

    try:
        dt = DeltaTable(GOLD_MONTHLY_SPEND_PATH)
        df = dt.to_pandas()
    except TableNotFoundError:
        # Gold hasn't been populated yet — return the same shape with zeros.
        return _empty_response(today_ym)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"delta read failed: {type(e).__name__}: {e}",
        )

    user_df = df[df["userId"] == x_user_id].sort_values("yearMonth", ascending=False)
    if user_df.empty:
        return _empty_response(today_ym)

    current_rows = user_df[user_df["yearMonth"] == today_ym]
    if not current_rows.empty:
        r = current_rows.iloc[0]
        current = {
            "yearMonth": today_ym,
            "totalSpend": float(r["totalSpend"]),
            "tripCount": int(r["tripCount"]),
            "avgTrip": float(r["avgTrip"]),
            "currency": str(r["currency"]),
        }
    else:
        # Use the currency from the user's most recent month, falling back
        # to the configured default. Avoids surfacing a hardcoded "MAD" for
        # users whose history is in a different currency.
        currency = str(user_df.iloc[0]["currency"]) if not user_df.empty else DEFAULT_CURRENCY
        current = {
            "yearMonth": today_ym,
            "totalSpend": 0.0,
            "tripCount": 0,
            "avgTrip": 0.0,
            "currency": currency,
        }

    history_df = user_df[user_df["yearMonth"] != today_ym].head(HISTORY_MONTHS)
    history = [
        {
            "yearMonth": str(r["yearMonth"]),
            "totalSpend": float(r["totalSpend"]),
            "tripCount": int(r["tripCount"]),
        }
        for _, r in history_df.iterrows()
    ]

    return {"currentMonth": current, "history": history}


def _empty_response(today_ym: str) -> dict:
    return {
        "currentMonth": {
            "yearMonth": today_ym,
            "totalSpend": 0.0,
            "tripCount": 0,
            "avgTrip": 0.0,
            "currency": DEFAULT_CURRENCY,
        },
        "history": [],
    }
