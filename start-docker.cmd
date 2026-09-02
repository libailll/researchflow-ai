@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

set "DOCKER_CMD=docker"
set "DOCKER_DESKTOP=%LOCALAPPDATA%\Programs\DockerDesktop\Docker Desktop.exe"
where docker >nul 2>nul
if errorlevel 1 set "DOCKER_CMD=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe"

if not exist "%DOCKER_CMD%" if "%DOCKER_CMD%" NEQ "docker" (
    echo [ResearchFlow] Docker command not found. Start or reinstall Docker Desktop and try again.
    pause
    exit /b 1
)

"%DOCKER_CMD%" info >nul 2>nul
if errorlevel 1 (
    if not exist "%DOCKER_DESKTOP%" (
        echo [ResearchFlow] Docker Desktop is not ready and its application was not found.
        pause
        exit /b 1
    )

    echo [ResearchFlow] Starting Docker Desktop...
    start "" "%DOCKER_DESKTOP%"
    set /a WAIT_COUNT=0

    :wait_for_docker
    timeout /t 3 /nobreak >nul
    "%DOCKER_CMD%" info >nul 2>nul
    if not errorlevel 1 goto docker_ready
    set /a WAIT_COUNT+=1
    if !WAIT_COUNT! GEQ 40 (
        echo [ResearchFlow] Docker Desktop did not become ready within 120 seconds.
        pause
        exit /b 1
    )
    goto wait_for_docker
)

:docker_ready
echo [ResearchFlow] Building and starting all services...
"%DOCKER_CMD%" compose up -d --build
if errorlevel 1 (
    echo [ResearchFlow] Startup failed. Review the error above.
    pause
    exit /b 1
)

"%DOCKER_CMD%" compose ps
echo.
echo [ResearchFlow] Started. Open http://localhost:3000
start "" "http://localhost:3000"
pause
