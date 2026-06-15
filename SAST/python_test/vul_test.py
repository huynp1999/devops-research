import sqlite3
from flask import Flask, request
from helpers import get_request_param, db_query
from executor import fetch_records
app = Flask(__name__)


@app.route("/search")
def search_endpoint():
    username = get_request_param("username")
    return str(fetch_records("users", {"username": username}))