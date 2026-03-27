@echo off
REM Script để khởi động AI Model API Server
REM Yêu cầu: Python 3.8+, pip install requirements

cd /d "%~dp0"

echo.
echo ========================================
echo  Warehouse AI Model API Server
echo ========================================
echo.

REM Kiểm tra Python installation
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Python is not installed or not in PATH
    pause
    exit /b 1
)

REM Kiểm tra virtual environment (tùy chọn)
if exist "venv\Scripts\activate.bat" (
    echo Activating virtual environment...
    call venv\Scripts\activate.bat
)

REM Cài đặt dependencies
echo.
echo Installing dependencies from requirements.txt...
pip install -q -r requirements.txt

if %errorlevel% neq 0 (
    echo Error installing dependencies
    pause
    exit /b 1
)

REM Chạy Flask server
echo.
echo ========================================
echo Starting AI Model API Server...
echo Server will be available at:
echo   http://localhost:5000
echo ========================================
echo.

python app.py

pause
