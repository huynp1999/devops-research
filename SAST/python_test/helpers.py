from flask import request


def get_request_param(param_name: str) -> str:
    """Get parameter from HTTP request"""
    return request.args.get(param_name)