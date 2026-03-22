@echo off
echo === Warehouse AI Forecasting System Setup ===

echo 1. Starting MySQL Database...
echo Please ensure MySQL is running on localhost:3306

echo 2. Training AI Model...
cd warehouse-ai
python main.py
cd ..

echo 3. Starting Backend Server...
start cmd /k "cd warehouse-be && mvn spring-boot:run"

echo 4. Starting Frontend Server...
start cmd /k "cd warehouse-fe && npm run dev"

echo.
echo === System Started ===
echo - AI Model: Trained and ready
echo - Backend API: http://localhost:9090
echo - Frontend: http://localhost:5173
echo.
echo Press any key to exit...
pause > nul