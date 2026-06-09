"""
Bootstrap script for the AtlasSync Superset dashboard.

Run once after `docker-compose up` to:
  1. Register the silver `line_items` dataset (gold `user_monthly_spend` is
     already registered by the README curl in #23's close comment).
  2. Create the 4 charts that make up the "AtlasSync — Overview" dashboard.
  3. Create the dashboard, attach the charts, set auto-refresh to 5 minutes.

Idempotent: re-running skips objects that already exist.

Usage (from host):
    python3 analytics/superset/bootstrap.py
"""
import json
import os
import sys
from typing import Optional

import requests

SUPERSET = os.getenv("SUPERSET_BASE_URL", "http://localhost:8088")
USERNAME = os.getenv("SUPERSET_USERNAME", "admin")
PASSWORD = os.getenv("SUPERSET_PASSWORD", "admin")

DASHBOARD_TITLE = "AtlasSync — Overview"
DASHBOARD_SLUG = "atlassync-overview"
REFRESH_FREQUENCY_SECONDS = 300

DB_NAME = "atlassync-trino"
GOLD_DATASET = "user_monthly_spend"
SILVER_DATASET = "line_items"


def main() -> None:
    s = requests.Session()
    login(s)

    db_id = find_database_id(s, DB_NAME)
    if db_id is None:
        sys.exit(f"database {DB_NAME!r} not found — run the Trino bootstrap first")
    print(f"using database id={db_id} name={DB_NAME}")

    gold_id = ensure_dataset(s, db_id, "atlassync", GOLD_DATASET)
    silver_id = ensure_dataset(s, db_id, "atlassync", SILVER_DATASET)
    print(f"datasets: gold id={gold_id} silver id={silver_id}")

    big_number_id = ensure_chart(s, "Total revenue this month",
                                  build_big_number_params(gold_id),
                                  gold_id, "big_number_total")
    revenue_line_id = ensure_chart(s, "Revenue by day (last 30 days)",
                                    build_revenue_line_params(silver_id),
                                    silver_id, "echarts_timeseries_line")
    shoppers_line_id = ensure_chart(s, "Active shoppers per day",
                                     build_shoppers_line_params(silver_id),
                                     silver_id, "echarts_timeseries_line")
    top_products_id = ensure_chart(s, "Top 10 products by revenue",
                                    build_top_products_params(silver_id),
                                    silver_id, "echarts_timeseries_bar")
    print(f"charts: big_number={big_number_id} revenue_line={revenue_line_id}"
          f" shoppers_line={shoppers_line_id} top_products={top_products_id}")

    dashboard_id = ensure_dashboard(
        s,
        DASHBOARD_TITLE,
        DASHBOARD_SLUG,
        [big_number_id, revenue_line_id, shoppers_line_id, top_products_id],
        REFRESH_FREQUENCY_SECONDS,
    )
    print(f"dashboard id={dashboard_id} at {SUPERSET}/superset/dashboard/{dashboard_id}/")


def login(s: requests.Session) -> None:
    r = s.post(f"{SUPERSET}/api/v1/security/login",
               json={"username": USERNAME, "password": PASSWORD, "provider": "db"})
    r.raise_for_status()
    token = r.json()["access_token"]
    s.headers["Authorization"] = f"Bearer {token}"
    csrf = s.get(f"{SUPERSET}/api/v1/security/csrf_token/").json()["result"]
    s.headers["X-CSRFToken"] = csrf
    s.headers["Referer"] = SUPERSET


def _rison_str(value: str) -> str:
    """Quote a value for rison-encoded filters. Strings with spaces or special
    chars must be wrapped in single quotes; literal single quotes are doubled."""
    return f"'{value.replace(chr(39), chr(39) * 2)}'"


def find_database_id(s: requests.Session, name: str) -> Optional[int]:
    r = s.get(f"{SUPERSET}/api/v1/database/", params={
        "q": f"(filters:!((col:database_name,opr:eq,value:{_rison_str(name)})))"
    })
    r.raise_for_status()
    rows = r.json()["result"]
    return rows[0]["id"] if rows else None


def find_dataset_id(s: requests.Session, schema: str, table: str) -> Optional[int]:
    r = s.get(f"{SUPERSET}/api/v1/dataset/", params={
        "q": f"(filters:!((col:table_name,opr:eq,value:{_rison_str(table)})))"
    })
    r.raise_for_status()
    for row in r.json()["result"]:
        if row.get("schema") == schema:
            return row["id"]
    return None


def ensure_dataset(s: requests.Session, db_id: int, schema: str, table: str) -> int:
    existing = find_dataset_id(s, schema, table)
    if existing is not None:
        return existing
    r = s.post(f"{SUPERSET}/api/v1/dataset/", json={
        "database": db_id, "schema": schema, "table_name": table,
    })
    r.raise_for_status()
    return r.json()["id"]


def find_chart_id(s: requests.Session, name: str) -> Optional[int]:
    r = s.get(f"{SUPERSET}/api/v1/chart/", params={
        "q": f"(filters:!((col:slice_name,opr:eq,value:{_rison_str(name)})))"
    })
    r.raise_for_status()
    rows = r.json()["result"]
    return rows[0]["id"] if rows else None


def ensure_chart(s: requests.Session, name: str, params: dict,
                 datasource_id: int, viz_type: str) -> int:
    payload = {
        "slice_name": name,
        "viz_type": viz_type,
        "datasource_id": datasource_id,
        "datasource_type": "table",
        "params": json.dumps(params),
    }
    existing = find_chart_id(s, name)
    if existing is not None:
        # Update params/viz_type in place so re-running the bootstrap picks up
        # config changes without manual chart deletion.
        r = s.put(f"{SUPERSET}/api/v1/chart/{existing}", json=payload)
        if not r.ok:
            print(f"chart update failed for {name!r}: {r.status_code} {r.text}")
            r.raise_for_status()
        return existing
    r = s.post(f"{SUPERSET}/api/v1/chart/", json=payload)
    if not r.ok:
        print(f"chart create failed for {name!r}: {r.status_code} {r.text}")
        r.raise_for_status()
    return r.json()["id"]


def ensure_dashboard(s: requests.Session, title: str, slug: str,
                     chart_ids: list[int], refresh_seconds: int) -> int:
    r = s.get(f"{SUPERSET}/api/v1/dashboard/", params={
        "q": f"(filters:!((col:slug,opr:eq,value:{_rison_str(slug)})))"
    })
    r.raise_for_status()
    existing_rows = r.json()["result"]
    if existing_rows:
        dashboard_id = existing_rows[0]["id"]
    else:
        r = s.post(f"{SUPERSET}/api/v1/dashboard/", json={
            "dashboard_title": title,
            "slug": slug,
            "published": True,
        })
        r.raise_for_status()
        dashboard_id = r.json()["id"]

    # Attach charts and set auto-refresh frequency on the dashboard's JSON metadata.
    json_meta = json.dumps({"refresh_frequency": refresh_seconds})
    s.put(f"{SUPERSET}/api/v1/dashboard/{dashboard_id}", json={
        "json_metadata": json_meta,
    })

    # Adding charts to a dashboard via the public API requires assembling the
    # `position_json` (the layout grid) — non-trivial. We instead attach each
    # chart to the dashboard through the chart's `dashboards` field so it shows
    # up in the dashboard's chart list. Users arrange charts in the UI.
    for chart_id in chart_ids:
        s.put(f"{SUPERSET}/api/v1/chart/{chart_id}", json={
            "dashboards": [dashboard_id],
        })

    return dashboard_id


# Trino lowercase-folds Delta column names, so we reference columns in lowercase
# everywhere even though Spark wrote them camelCase.

def build_big_number_params(dataset_id: int) -> dict:
    """Total revenue this month — sum(totalspend) from gold for current YYYY-MM."""
    return {
        "datasource": f"{dataset_id}__table",
        "viz_type": "big_number_total",
        "metric": {
            "label": "Total revenue",
            "expressionType": "SIMPLE",
            "aggregate": "SUM",
            "column": {"column_name": "totalspend"},
        },
        "adhoc_filters": [{
            "clause": "WHERE",
            "expressionType": "SQL",
            "sqlExpression": "yearmonth = date_format(current_date, '%Y-%m')",
        }],
        "subheader": "Current calendar month, MAD",
    }


def build_revenue_line_params(dataset_id: int) -> dict:
    """Revenue by day, last 30 days — line chart from silver."""
    return {
        "datasource": f"{dataset_id}__table",
        "viz_type": "echarts_timeseries_line",
        "x_axis": "eventdate",
        "time_grain_sqla": "P1D",
        "metrics": [{
            "label": "Revenue",
            "expressionType": "SIMPLE",
            "aggregate": "SUM",
            "column": {"column_name": "linetotal"},
        }],
        "adhoc_filters": [{
            "clause": "WHERE",
            "expressionType": "SQL",
            "sqlExpression": "eventdate >= current_date - INTERVAL '30' DAY",
        }],
        "row_limit": 1000,
    }


def build_shoppers_line_params(dataset_id: int) -> dict:
    """Active shoppers per day — count_distinct(userid) by eventdate."""
    return {
        "datasource": f"{dataset_id}__table",
        "viz_type": "echarts_timeseries_line",
        "x_axis": "eventdate",
        "time_grain_sqla": "P1D",
        "metrics": [{
            "label": "Active shoppers",
            "expressionType": "SIMPLE",
            "aggregate": "COUNT_DISTINCT",
            "column": {"column_name": "userid"},
        }],
        "adhoc_filters": [{
            "clause": "WHERE",
            "expressionType": "SQL",
            "sqlExpression": "eventdate >= current_date - INTERVAL '30' DAY",
        }],
        "row_limit": 1000,
    }


def build_top_products_params(dataset_id: int) -> dict:
    """Top 10 products by revenue — bar chart from silver."""
    return {
        "datasource": f"{dataset_id}__table",
        "viz_type": "echarts_timeseries_bar",
        "x_axis": "name",
        "metrics": [{
            "label": "Revenue",
            "expressionType": "SIMPLE",
            "aggregate": "SUM",
            "column": {"column_name": "linetotal"},
        }],
        "row_limit": 10,
        "orderby": [[{
            "label": "Revenue",
            "expressionType": "SIMPLE",
            "aggregate": "SUM",
            "column": {"column_name": "linetotal"},
        }, False]],  # desc
    }


if __name__ == "__main__":
    main()
