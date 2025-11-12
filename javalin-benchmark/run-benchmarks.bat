@echo off
REM Javalin Benchmark Runner Script for Windows
REM This script provides convenient ways to run benchmarks with common configurations

setlocal enabledelayedexpansion

set BENCHMARK_JAR=target\benchmarks.jar
set OUTPUT_DIR=benchmark-results
set TIMESTAMP=%date:~-4%%date:~-10,2%%date:~-7,2%-%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM Check if JAR exists
if not exist "%BENCHMARK_JAR%" (
    echo [ERROR] Benchmark JAR not found at %BENCHMARK_JAR%
    echo [INFO] Building benchmark JAR...
    call mvn clean package
    if errorlevel 1 (
        echo [ERROR] Build failed
        exit /b 1
    )
)

REM Create output directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Parse command
set COMMAND=%1
if "%COMMAND%"=="" set COMMAND=all

if "%COMMAND%"=="all" goto run_all
if "%COMMAND%"=="routing" goto run_routing
if "%COMMAND%"=="static" goto run_static
if "%COMMAND%"=="context" goto run_context
if "%COMMAND%"=="e2e" goto run_e2e
if "%COMMAND%"=="quick" goto run_quick
if "%COMMAND%"=="-h" goto usage
if "%COMMAND%"=="--help" goto usage

echo [ERROR] Unknown command: %COMMAND%
goto usage

:run_all
echo [INFO] Running all benchmarks...
set OUTPUT_FILE=%OUTPUT_DIR%\all-benchmarks-%TIMESTAMP%.json
java -jar "%BENCHMARK_JAR%" -rf json -rff "%OUTPUT_FILE%"
echo [INFO] Results saved to: %OUTPUT_FILE%
goto end

:run_routing
echo [INFO] Running routing benchmarks...
set OUTPUT_FILE=%OUTPUT_DIR%\routing-%TIMESTAMP%.json
java -jar "%BENCHMARK_JAR%" RoutingBenchmark -rf json -rff "%OUTPUT_FILE%"
echo [INFO] Results saved to: %OUTPUT_FILE%
goto end

:run_static
echo [INFO] Running static file benchmarks...
set OUTPUT_FILE=%OUTPUT_DIR%\static-%TIMESTAMP%.json
java -jar "%BENCHMARK_JAR%" StaticFilesBenchmark -rf json -rff "%OUTPUT_FILE%"
echo [INFO] Results saved to: %OUTPUT_FILE%
goto end

:run_context
echo [INFO] Running context methods benchmarks...
set OUTPUT_FILE=%OUTPUT_DIR%\context-%TIMESTAMP%.json
java -jar "%BENCHMARK_JAR%" ContextMethodsBenchmark -rf json -rff "%OUTPUT_FILE%"
echo [INFO] Results saved to: %OUTPUT_FILE%
goto end

:run_e2e
echo [INFO] Running end-to-end benchmarks...
set OUTPUT_FILE=%OUTPUT_DIR%\e2e-%TIMESTAMP%.json
java -jar "%BENCHMARK_JAR%" EndToEndBenchmark -rf json -rff "%OUTPUT_FILE%"
echo [INFO] Results saved to: %OUTPUT_FILE%
goto end

:run_quick
echo [INFO] Running quick benchmarks (reduced iterations)...
set OUTPUT_FILE=%OUTPUT_DIR%\quick-%TIMESTAMP%.json
java -jar "%BENCHMARK_JAR%" -wi 1 -i 2 -f 1 -rf json -rff "%OUTPUT_FILE%"
echo [INFO] Results saved to: %OUTPUT_FILE%
goto end

:usage
echo Usage: %0 [COMMAND]
echo.
echo Commands:
echo     all             Run all benchmarks (default)
echo     routing         Run routing benchmarks only
echo     static          Run static file benchmarks only
echo     context         Run context methods benchmarks only
echo     e2e             Run end-to-end benchmarks only
echo     quick           Run quick benchmarks (fewer iterations)
echo.
echo Options:
echo     -h, --help      Show this help message
echo.
echo Examples:
echo     %0 all          # Run all benchmarks
echo     %0 routing      # Run routing benchmarks
echo     %0 quick        # Quick run for development
goto end

:end
endlocal

