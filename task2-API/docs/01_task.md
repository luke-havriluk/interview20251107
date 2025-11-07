Currency Data Fetcher - Simple Interview Challenge
================================================

Your task is to implement a simple method that fetches all exchange rates
for a given date from the Frankfurter API.

API Documentation: https://frankfurter.dev/
Base URL: https://api.frankfurter.dev/v1

Time allocation: 20-30 minutes
"""

import requests
from typing import Dict, Optional


def get_currencies_by_date(date: Optional[str] = None) -> Dict:
    """
    Fetch all exchange rates for a specific date from Frankfurter API.
    
    Args:
        date (str, optional): Date in YYYY-MM-DD format (e.g., "2024-01-15").
                             If None, returns the latest available rates.
    
    Returns:
        Dict: API response containing all exchange rates
        
    Examples:
        # Get latest rates
        get_currencies_by_date()
        # Returns: {"base": "EUR", "date": "2024-11-25", "rates": {"USD": 1.0584, "GBP": 0.8347, ...}}
        
        # Get historical rates
        get_currencies_by_date("2024-01-15") 
        # Returns: {"base": "EUR", "date": "2024-01-15", "rates": {"USD": 1.0891, "GBP": 0.8572, ...}}
    
    Raises:
        Exception: For any API or network errors
    """
    
    # TODO: Implement this function
    #
    # Requirements:
    # 1. Build the correct API URL:
    #    - If date is None: https://api.frankfurter.dev/v1/latest
    #    - If date provided: https://api.frankfurter.dev/v1/{date}
    #
    # 2. Make HTTP GET request using requests.get()
    #
    # 3. Handle errors:
    #    - Network errors (connection issues)
    #    - API errors (invalid date, API down)
    #    - Check if response is successful (status code 200)
    #
    # 4. Return the JSON response as a dictionary
    #
    # Hints:
    # - Use f-strings to build URLs
    # - Use response.json() to parse JSON
    # - Use response.raise_for_status() to check for HTTP errors
    # - The API returns all currencies by default (no filtering needed)
    




