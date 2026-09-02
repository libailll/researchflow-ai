@echo off
setlocal
cd /d "%~dp0"

set "DOCKER_CMD=docker"
where docker >nul 2>nul
if errorlevel 1 set "DOCKER_CMD=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe"

if not exist "%DOCKER_CMD%" if "%DOCKER_CMD%" NEQ "docker" (
    echo [ResearchFlow] Docker command not found.
    pause
    exit /b 1
)

"%DOCKER_CMD%" compose down
if errorlevel 1 (
    echo [ResearchFlow] Stop failed. Review the error above.
    pause
    exit /b 1
)

echo [ResearchFlow] Services stopped. Persistent data was preserved.
pause
