"""
Smoke test DAG. Trigger manually from the UI or CLI to confirm Airflow
parses files in /opt/airflow/dags and can execute a BashOperator.
Delete once the real DAGs (#22) are wired up.
"""
from datetime import datetime

from airflow import DAG
from airflow.operators.bash import BashOperator

with DAG(
    dag_id="smoke_test",
    description="Trivial echo DAG to verify Airflow plumbing.",
    schedule=None,
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["smoke"],
) as dag:
    BashOperator(
        task_id="say_hello",
        bash_command='echo "hello from atlassync airflow"',
    )
