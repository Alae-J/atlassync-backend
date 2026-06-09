"""
Structured Streaming: purchases.completed (Kafka) -> bronze Delta on HDFS.

Bronze is intentionally schema-on-read: we store the raw JSON value as a
string alongside Kafka envelope metadata. A schema change in the producer
will never break this job — silver is where types get enforced.

Run from inside the spark-master container:

    /opt/spark/bin/spark-submit \\
        --master spark://spark-master:7077 \\
        --conf spark.jars.ivy=/tmp/.ivy2 \\
        --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.8,io.delta:delta-spark_2.12:3.2.0 \\
        --conf spark.sql.extensions=io.delta.sql.DeltaSparkSessionExtension \\
        --conf spark.sql.catalog.spark_catalog=org.apache.spark.sql.delta.catalog.DeltaCatalog \\
        /opt/atlassync/spark/jobs/stream_purchases_to_bronze.py
"""

from pyspark.sql import SparkSession
from pyspark.sql.functions import col, current_date

KAFKA_BOOTSTRAP = "kafka:29092"
TOPIC = "purchases.completed"
BRONZE_PATH = "hdfs://hadoop-namenode:9000/atlassync/bronze/purchases"
CHECKPOINT_PATH = "hdfs://hadoop-namenode:9000/atlassync/checkpoints/bronze-purchases"


def main() -> None:
    spark = (
        SparkSession.builder
        .appName("stream_purchases_to_bronze")
        .getOrCreate()
    )

    raw = (
        spark.readStream.format("kafka")
        .option("kafka.bootstrap.servers", KAFKA_BOOTSTRAP)
        .option("subscribe", TOPIC)
        .option("startingOffsets", "earliest")
        .option("failOnDataLoss", "false")
        .load()
    )

    bronze = (
        raw
        .selectExpr(
            "CAST(key AS STRING) AS session_id",
            "CAST(value AS STRING) AS payload_json",
            "topic",
            "partition",
            "offset",
            "timestamp AS kafka_ts",
        )
        .withColumn("event_date", current_date())
    )

    query = (
        bronze.writeStream
        .format("delta")
        .outputMode("append")
        .option("checkpointLocation", CHECKPOINT_PATH)
        .partitionBy("event_date")
        .start(BRONZE_PATH)
    )

    query.awaitTermination()


if __name__ == "__main__":
    main()
