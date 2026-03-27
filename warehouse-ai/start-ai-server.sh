#!/bin/bash
# Script để khởi động AI Model API Server trên Linux/Mac
# Yêu cầu: Python 3.8+, pip install requirements

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo ""
echo "========================================"
echo "  Warehouse AI Model API Server"
echo "========================================"
echo ""

# Kiểm tra Python installation
if ! command -v python3 &> /dev/null && ! command -v python &> /dev/null; then
    echo "Error: Python is not installed"
    exit 1
fi

# Xác định Python command
if command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
else
    PYTHON_CMD="python"
fi

# Kiểm tra virtual environment
if [ -f "venv/bin/activate" ]; then
    echo "Activating virtual environment..."
    source venv/bin/activate
fi

# Cài đặt dependencies
echo ""
echo "Installing dependencies from requirements.txt..."
$PYTHON_CMD -m pip install -q -r requirements.txt

if [ $? -ne 0 ]; then
    echo "Error installing dependencies"
    exit 1
fi

# Chạy Flask server
echo ""
echo "========================================"
echo "Starting AI Model API Server..."
echo "Server will be available at:"
echo "  http://localhost:5000"
echo "========================================"
echo ""

$PYTHON_CMD app.py
