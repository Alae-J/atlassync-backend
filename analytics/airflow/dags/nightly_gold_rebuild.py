"""
DAG 2 — Nightly gold rebuild.

Daily at 02:00 UTC: pulls fresh data from bronze into silver, then re-runs
the silver -> gold aggregations. Both Spark jobs are idempotent (silver
MERGEs on (eventId, barcode); gold overwrites the whole table), so reruns
do not duplicate rows.

The dashboard-refresh task is a placeholder until Superset lands (#24).
"""
from datetime import datetime

from airflow import DAG
from airflow.operators.bash import BashOperator

SPARK_SUBMIT_PREFIX = (
    "docker exec atlassync-backend_spark-master_1 "
    "/opt/spark/bin/spark-submit "
    "--master spark://spark-master:7077 "
    "--conf spark.jars.ivy=/tmp/.ivy2 "
    "--conf spark.sql.extensions=io.delta.sql.DeltaSparkSessionExtension "
    "--conf spark.sql.catalog.spark_catalog="
    "org.apache.spark.sql.delta.catalog.DeltaCatalog"
)

with DAG(
    dag_id="nightly_gold_rebuild",
    description="Rebuild silver + gold from bronze, refresh dashboards.",
    schedule="0 2 * * *",
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["bigdata"],
) as dag:
    check_new_bronze = BashOperator(
        task_id="check_new_bronze",
        bash_command=(
            "docker exec atlassync-backend_hadoop-datanode_1 "
            "hdfs dfs -test -d /atlassync/bronze/purchases && "
            "docker exec atlassync-backend_hadoop-datanode_1 "
            "hdfs dfs -count /atlassync/bronze/purchases"
        ),
    )

    bronze_to_silver = BashOperator(
        task_id="bronze_to_silver",
        bash_command=(
            f"{SPARK_SUBMIT_PREFIX} "
            "--packages io.delta:delta-spark_2.12:3.2.0,"
            "org.postgresql:postgresql:42.7.3 "
            "/opt/atlassync/spark/jobs/bronze_to_silver_line_items.py"
        ),
    )

    silver_to_gold = BashOperator(
        task_id="silver_to_gold",
        bash_command=(
            f"{SPARK_SUBMIT_PREFIX} "
            "--packages io.delta:delta-spark_2.12:3.2.0 "
            "/opt/atlassync/spark/jobs/silver_to_gold_monthly_spend.py"
        ),
    )

    refresh_superset_dashboard = BashOperator(
        task_id="refresh_superset_dashboard",
        bash_command='echo "TODO: wire Superset refresh in #24 once the dashboard exists"',
    )

    check_new_bronze >> bronze_to_silver >> silver_to_gold >> refresh_superset_dashboard
