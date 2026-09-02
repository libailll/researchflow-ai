@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
title ResearchFlow AI Service
cd /d "%~dp0"

if not exist ".venv\Scripts\python.exe" (
  echo [ERROR] AI Service dependencies are missing. Run setup.cmd first.
  echo.
  pause
  exit /b 1
)

set "PORT_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8090" ^| findstr "LISTENING"') do set "PORT_PID=%%P"

if defined PORT_PID (
  echo Port 8090 is already used by process !PORT_PID!.
  choice /C YN /N /M "Stop the old AI Service and restart now? [Y/N] "
  if errorlevel 2 exit /b 0

  taskkill /PID !PORT_PID! /F >nul 2>&1
  if errorlevel 1 (
    echo.
    echo [ERROR] Cannot stop process !PORT_PID!. Run this file as administrator or stop it manually.
    echo.
    pause
    exit /b 1
  )
  timeout /t 1 /nobreak >nul
  echo Old AI Service stopped. Starting the new version...
  echo.
)

echo Starting ResearchFlow AI Service...
echo Service URL: http://127.0.0.1:8090
echo Press Ctrl+C to stop the service.
echo.

".venv\Scripts\python.exe" -m uvicorn app.main:app --host 127.0.0.1 --port 8090
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
  echo.
  echo [ERROR] AI Service stopped with exit code %EXIT_CODE%.
  echo Review the error details above before closing this window.
  echo.
  pause
)
exit /b %EXIT_CODE%
