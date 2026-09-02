@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
title Stop ResearchFlow AI Service

set "PORT_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8090" ^| findstr "LISTENING"') do set "PORT_PID=%%P"

if not defined PORT_PID (
  echo AI Service is not running. Port 8090 is free.
  echo.
  pause
  exit /b 0
)

echo Stopping AI Service process !PORT_PID! ...
taskkill /PID !PORT_PID! /F >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Cannot stop the process. Run this file as administrator.
) else (
  echo AI Service stopped.
)
echo.
pause
