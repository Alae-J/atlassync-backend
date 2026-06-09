"""
DAG 1 — Streaming setup.

Manual-trigger DAG that brings up the Kafka -> bronze streaming job after
the docker stack starts. Verifies the broker is reachable + the producer
topic exists, then fires the streaming Spark job in background mode on
spark-master.

Re-runnable: if the streaming job is already running, the spark-submit
launches a second driver alongside the first — fine for dev, the
Standalone master tolerates it. In prod, this DAG would have a
\"streaming-job-already-up\" sensor as the first task.
"""
from datetime import datetime

from airflow import DAG
from airflow.operators.bash import BashOperator

with DAG(
    dag_id="streaming_setup",
    description="Bring up the Kafka -> bronze Delta streaming job.",
    schedule=None,
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["bigdata"],
) as dag:
    kafka_check = BashOperator(
        task_id="kafka_check",
        bash_command=(
            "docker exec atlassync-backend_kafka_1 "
            "kafka-topics --bootstrap-server localhost:9092 --list"
        ),
    )

    producer_check = BashOperator(
        task_id="producer_check",
        bash_command=(
            "docker exec atlassync-backend_kafka_1 "
            "kafka-topics --bootstrap-server localhost:9092 "
            "--describe --topic purchases.completed"
        ),
    )

    start_spark_streaming_job = BashOperator(
        task_id="start_spark_streaming_job",
        bash_command=(
            "docker exec -d atlassync-backend_spark-master_1 "
            "/opt/spark/bin/spark-submit "
            "--master spark://spark-master:7077 "
            "--conf spark.jars.ivy=/tmp/.ivy2 "
            "--packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.8,"
            "io.delta:delta-spark_2.12:3.2.0 "
            "--conf spark.sql.extensions=io.delta.sql.DeltaSparkSessionExtension "
            "--conf spark.sql.catalog.spark_catalog="
            "org.apache.spark.sql.delta.catalog.DeltaCatalog "
            "/opt/atlassync/spark/jobs/stream_purchases_to_bronze.py"
        ),
    )

    kafka_check >> producer_check >> start_spark_streaming_job
