"""
Batch: bronze.purchases (raw JSON) -> silver.line_items (typed, enriched).

Each row in silver is a single (eventId, barcode) line-item. categoryId and
brand are joined from product-service Postgres at run time — they're not in
the bronze event by design (keeps session-service decoupled from product).

Idempotent via Delta MERGE on (eventId, barcode): re-running over the same
bronze partition replaces the matching rows instead of duplicating them.

Run from inside the spark-master container:

    /opt/spark/bin/spark-submit \\
        --master spark://spark-master:7077 \\
        --conf spark.jars.ivy=/tmp/.ivy2 \\
        --packages io.delta:delta-spark_2.12:3.2.0,org.postgresql:postgresql:42.7.3 \\
        --conf spark.sql.extensions=io.delta.sql.DeltaSparkSessionExtension \\
        --conf spark.sql.catalog.spark_catalog=org.apache.spark.sql.delta.catalog.DeltaCatalog \\
        /opt/atlassync/spark/jobs/bronze_to_silver_line_items.py
"""

from delta.tables import DeltaTable
from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    broadcast,
    col,
    explode,
    from_json,
    to_date,
    to_timestamp,
)
from pyspark.sql.types import (
    ArrayType,
    DecimalType,
    IntegerType,
    LongType,
    StringType,
    StructField,
    StructType,
)

BRONZE_PATH = "hdfs://hadoop-namenode:9000/atlassync/bronze/purchases"
SILVER_PATH = "hdfs://hadoop-namenode:9000/atlassync/silver/line_items"

PRODUCTS_JDBC_URL = "jdbc:postgresql://postgres:5432/atlassync_product"
PRODUCTS_JDBC_PROPS = {
    "user": "atlassync",
    "password": "atlassync",
    "driver": "org.postgresql.Driver",
}

# Schema of the JSON payload published by session-service on purchases.completed.
PAYLOAD_SCHEMA = StructType([
    StructField("eventId",    StringType()),
    StructField("eventTime",  StringType()),
    StructField("sessionId",  StringType()),
    StructField("userId",     LongType()),
    StructField("storeId",    LongType()),
    StructField("currency",   StringType()),
    StructField("total",      DecimalType(12, 4)),
    StructField("items", ArrayType(StructType([
        StructField("barcode",   StringType()),
        StructField("name",      StringType()),
        StructField("unitPrice", DecimalType(12, 4)),
        StructField("qty",       IntegerType()),
    ]))),
])


def main() -> None:
    spark = (
        SparkSession.builder
        .appName("bronze_to_silver_line_items")
        .getOrCreate()
    )

    bronze = (
        spark.read.format("delta").load(BRONZE_PATH)
        .withColumn("payload", from_json(col("payload_json"), PAYLOAD_SCHEMA))
    )

    exploded = (
        bronze
        .select(
            col("payload.eventId").alias("eventId"),
            to_timestamp(col("payload.eventTime")).alias("eventTime"),
            col("payload.sessionId").alias("sessionId"),
            col("payload.userId").alias("userId"),
            col("payload.storeId").alias("storeId"),
            col("payload.currency").alias("currency"),
            explode(col("payload.items")).alias("item"),
        )
        .select(
            "eventId",
            "eventTime",
            "sessionId",
            "userId",
            "storeId",
            "currency",
            col("item.barcode").alias("barcode"),
            col("item.name").alias("name"),
            col("item.unitPrice").alias("unitPrice"),
            col("item.qty").alias("qty"),
        )
        .withColumn("lineTotal", col("unitPrice") * col("qty"))
        .withColumn("eventDate", to_date(col("eventTime")))
    )

    products = (
        spark.read.jdbc(
            url=PRODUCTS_JDBC_URL,
            table="(SELECT barcode, brand, category_id FROM products) AS p",
            properties=PRODUCTS_JDBC_PROPS,
        )
        .withColumnRenamed("category_id", "categoryId")
    )

    enriched = (
        exploded
        .join(broadcast(products), on="barcode", how="left")
        .select(
            "eventId",
            "eventTime",
            "sessionId",
            "userId",
            "storeId",
            "barcode",
            "name",
            "categoryId",
            "brand",
            "unitPrice",
            "qty",
            "lineTotal",
            "currency",
            "eventDate",
        )
    )

    if DeltaTable.isDeltaTable(spark, SILVER_PATH):
        target = DeltaTable.forPath(spark, SILVER_PATH)
        (
            target.alias("t")
            .merge(enriched.alias("s"), "t.eventId = s.eventId AND t.barcode = s.barcode")
            .whenMatchedUpdateAll()
            .whenNotMatchedInsertAll()
            .execute()
        )
    else:
        (
            enriched.write
            .format("delta")
            .partitionBy("eventDate")
            .save(SILVER_PATH)
        )

    spark.stop()


if __name__ == "__main__":
    main()
