@echo off
:: 🚀 Booky Backend - Docker Runner Script for Windows
:: Este script facilita la ejecución de la aplicación con Docker en Windows

setlocal enabledelayedexpansion

echo 🚀 Booky Backend - Docker Runner
echo =================================
echo.

:: Check if Docker is running
docker info >nul 2>&1
if !errorlevel! neq 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop and try again.
    pause
    exit /b 1
)
echo [SUCCESS] Docker is running ✓
echo.

:menu
echo Select an option:
echo 1^) 🚀 Start application
echo 2^) 🛑 Stop application
echo 3^) 🔄 Restart application
echo 4^) 🔨 Rebuild from scratch
echo 5^) ⚡ Rebuild app only (fast)
echo 6^) 📋 Show status
echo 7^) 📜 Show app logs
echo 8^) 🗄️  Show database logs
echo 9^) 🧹 Clean up
echo 10^) ❌ Exit
echo.

set /p choice="Enter your choice [1-10]: "

if "%choice%"=="1" goto start_app
if "%choice%"=="2" goto stop_app
if "%choice%"=="3" goto restart_app
if "%choice%"=="4" goto rebuild_app
if "%choice%"=="5" goto rebuild_app_only
if "%choice%"=="6" goto show_status
if "%choice%"=="7" goto show_logs
if "%choice%"=="8" goto show_db_logs
if "%choice%"=="9" goto cleanup
if "%choice%"=="10" goto exit
echo [ERROR] Invalid option. Please try again.
echo.
goto menu

:start_app
echo [INFO] Starting Booky Backend application...
docker-compose up --build -d
if !errorlevel! neq 0 (
    echo [ERROR] Failed to start containers
    pause
    goto menu
)
echo [SUCCESS] Containers started!
echo [INFO] Waiting for services to be ready...

:: Wait a bit for services to initialize
timeout /t 10 /nobreak >nul

echo.
echo [SUCCESS] 🎉 Booky Backend is ready!
echo.
echo 📋 Service URLs:
echo    🚀 API:          http://localhost:8080
echo    📚 Swagger UI:   http://localhost:8080/swagger-ui/index.html
echo    🗄️  Adminer:      http://localhost:8081
echo    📊 Database:     localhost:5433 (postgres/admin)
echo.
echo 🧪 Test the API:
echo    curl http://localhost:8080/api/books/search?q=test
echo.
pause
goto menu

:stop_app
echo [INFO] Stopping Booky Backend application...
docker-compose down
echo [SUCCESS] Application stopped ✓
echo.
pause
goto menu

:restart_app
echo [INFO] Restarting Booky Backend application...
docker-compose down
docker-compose up --build -d
echo [SUCCESS] Application restarted ✓
echo.
pause
goto menu

:rebuild_app
echo [INFO] Rebuilding Booky Backend from scratch...
echo [INFO] Stopping and removing containers, images, and volumes...
docker-compose down -v --rmi all --remove-orphans
echo [INFO] Pruning Docker system...
docker system prune -f
echo [INFO] Building and starting fresh containers...
docker-compose up --build -d
echo [SUCCESS] Application rebuilt from scratch ✓
echo.
pause
goto menu

:rebuild_app_only
echo [INFO] Rebuilding only Booky App (keeping database)...
echo [INFO] Stopping booky-app container...
docker-compose stop booky-app
echo [INFO] Removing booky-app container...
docker-compose rm -f booky-app
echo [INFO] Rebuilding booky-app image...
docker-compose build --no-cache booky-app
echo [INFO] Starting booky-app container...
docker-compose up -d booky-app
echo [SUCCESS] Booky App rebuilt and restarted ✓
echo.
pause
goto menu

:show_status
echo [INFO] Docker containers status:
docker-compose ps
echo.
pause
goto menu

:show_logs
echo [INFO] Showing application logs (Press Ctrl+C to exit)...
docker-compose logs -f booky-app
goto menu

:show_db_logs
echo [INFO] Showing database logs (Press Ctrl+C to exit)...
docker-compose logs -f postgres
goto menu

:cleanup
echo [INFO] Cleaning up Docker resources...
docker-compose down -v --remove-orphans
docker system prune -f
echo [SUCCESS] Cleanup completed ✓
echo.
pause
goto menu

:exit
echo [SUCCESS] Goodbye! 👋
pause
exit /b 0 