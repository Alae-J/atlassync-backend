"""
Batch: silver.line_items -> gold.user_monthly_spend.

Aggregates each user's purchases by (year, month). The mobile Profile tab's
"Spent X this month" stat reads directly from this table via analytics-api.

Gold tables are fully derived from silver, so this job overwrites the whole
table on each run — simplest correctness model, zero merge logic to maintain.
Cheap because the cardinality is small (users × months).

Run from inside the spark-master container:

    /opt/spark/bin/spark-submit \\
        --master spark://spark-master:7077 \\
        --conf spark.jars.ivy=/tmp/.ivy2 \\
        --packages io.delta:delta-spark_2.12:3.2.0 \\
        --conf spark.sql.extensions=io.delta.sql.DeltaSparkSessionExtension \\
        --conf spark.sql.catalog.spark_catalog=org.apache.spark.sql.delta.catalog.DeltaCatalog \\
        /opt/atlassync/spark/jobs/silver_to_gold_monthly_spend.py
"""

from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    col,
    countDistinct,
    date_format,
    sum as _sum,
)

SILVER_PATH = "hdfs://hadoop-namenode:9000/atlassync/silver/line_items"
GOLD_PATH = "hdfs://hadoop-namenode:9000/atlassync/gold/user_monthly_spend"


def main() -> None:
    spark = (
        SparkSession.builder
        .appName("silver_to_gold_monthly_spend")
        .getOrCreate()
    )

    silver = spark.read.format("delta").load(SILVER_PATH)

    gold = (
        silver
        .withColumn("yearMonth", date_format(col("eventDate"), "yyyy-MM"))
        .groupBy("userId", "yearMonth", "currency")
        .agg(
            _sum("lineTotal").alias("totalSpend"),
            countDistinct("sessionId").alias("tripCount"),
        )
        .withColumn("avgTrip", col("totalSpend") / col("tripCount"))
        .select(
            "userId",
            "yearMonth",
            "totalSpend",
            "tripCount",
            "avgTrip",
            "currency",
        )
    )

    (
        gold.write
        .format("delta")
        .mode("overwrite")
        .save(GOLD_PATH)
    )

    spark.stop()


if __name__ == "__main__":
    main()
