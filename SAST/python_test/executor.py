import sqlite3


def fetch_records(table: str, filters: dict) -> list:
    """Fetch records from database with filters"""
    conn = sqlite3.connect("app.db")
    cursor = conn.cursor()
    conditions = " AND ".join([f"{k} = '{v}'" for k, v in filters.items()])
    stmt = f"SELECT * FROM {table} WHERE {conditions}"
    cursor.execute(stmt)
    result = cursor.fetchall()
    conn.close()
    return result