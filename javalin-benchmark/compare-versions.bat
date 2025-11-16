@echo off
setlocal enabledelayedexpansion

REM Default values
set VERSION_1=
set VERSION_2=
set BENCHMARK_PATTERN=.*
set WARMUP_ITERATIONS=3
set MEASUREMENT_ITERATIONS=5
set FORKS=1
set OUTPUT_DIR=benchmark-results

REM Parse command line arguments
:parse_args
if "%~1"=="" goto validate_args
if "%~1"=="-v1" (
    set VERSION_1=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="-v2" (
    set VERSION_2=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="-b" (
    set BENCHMARK_PATTERN=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="-w" (
    set WARMUP_ITERATIONS=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="-m" (
    set MEASUREMENT_ITERATIONS=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="-f" (
    set FORKS=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="-o" (
    set OUTPUT_DIR=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="-h" goto usage
if "%~1"=="--help" goto usage
echo Unknown option: %~1
goto usage

:validate_args
if "%VERSION_1%"=="" (
    echo Error: -v1 is required
    goto usage
)
if "%VERSION_2%"=="" (
    echo Error: -v2 is required
    goto usage
)

echo.
echo ========================================
echo Javalin Performance Comparison
echo ========================================
echo Version 1: %VERSION_1%
echo Version 2: %VERSION_2%
echo Benchmark pattern: %BENCHMARK_PATTERN%
echo Warmup iterations: %WARMUP_ITERATIONS%
echo Measurement iterations: %MEASUREMENT_ITERATIONS%
echo Forks: %FORKS%
echo Output directory: %OUTPUT_DIR%
echo.

REM Create output directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Clean the benchmark module once before building
echo.
echo Cleaning benchmark module...
call mvn clean -pl javalin-benchmark -q

REM Build and run benchmarks for version 1
echo.
echo ========================================
echo Building and running: Javalin %VERSION_1%
echo ========================================
call :build_and_run "%VERSION_1%"
if errorlevel 1 exit /b 1

REM Build and run benchmarks for version 2
echo.
echo ========================================
echo Building and running: Javalin %VERSION_2%
echo ========================================
call :build_and_run "%VERSION_2%"
if errorlevel 1 exit /b 1

REM Generate comparison report
echo.
echo ========================================
echo Comparison Results
echo ========================================
echo.

REM Parse and display results using PowerShell
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$v1 = Get-Content '%OUTPUT_DIR%\results-%VERSION_1%.json' | ConvertFrom-Json; " ^
    "$v2 = Get-Content '%OUTPUT_DIR%\results-%VERSION_2%.json' | ConvertFrom-Json; " ^
    "Write-Host ''; " ^
    "Write-Host ('=' * 100); " ^
    "Write-Host ('{0,-60} {1,15} {2,15} {3,10}' -f 'Benchmark', '%VERSION_1%', '%VERSION_2%', 'Change'); " ^
    "Write-Host ('=' * 100); " ^
    "for ($i = 0; $i -lt $v1.Count; $i++) { " ^
        "$name = $v1[$i].benchmark -replace 'io.javalin.benchmark.', ''; " ^
        "$score1 = [math]::Round($v1[$i].primaryMetric.score, 3); " ^
        "$score2 = [math]::Round($v2[$i].primaryMetric.score, 3); " ^
        "$unit = $v1[$i].primaryMetric.scoreUnit; " ^
        "$change = if ($score1 -gt 0) { [math]::Round((($score2 - $score1) / $score1) * 100, 2) } else { 0 }; " ^
        "$changeStr = if ($change -gt 0) { '+' + [string]$change + '%%' } elseif ($change -lt 0) { [string]$change + '%%' } else { 'no change' }; " ^
        "Write-Host ('{0,-60} {1,12} {2} {3,12} {4} {5,10}' -f $name, $score1, $unit, $score2, $unit, $changeStr); " ^
    "}; " ^
    "Write-Host ('=' * 100); " ^
    "Write-Host ''"

echo.
echo Results also saved to:
echo   %OUTPUT_DIR%\results-%VERSION_1%.json
echo   %OUTPUT_DIR%\results-%VERSION_2%.json
echo.
echo For visual comparison, upload JSON files to: https://jmh.morethan.io/
echo.
echo ========================================
echo Comparison Complete!
echo ========================================
goto :eof

:build_and_run
set BUILD_VERSION=%~1
echo.
echo [%BUILD_VERSION%] Building benchmark JAR...

REM Try to use predefined profile first
call mvn package -pl javalin-benchmark -P javalin-%BUILD_VERSION% -DskipTests -q 2>nul
if errorlevel 1 (
    REM Fall back to custom version
    echo [%BUILD_VERSION%] Using custom version profile...
    call mvn package -pl javalin-benchmark -Djavalin.custom.version=%BUILD_VERSION% -DskipTests -q
    if errorlevel 1 (
        echo [%BUILD_VERSION%] ERROR: Build failed
        exit /b 1
    )
)

echo [%BUILD_VERSION%] Build complete

echo [%BUILD_VERSION%] Running benchmarks...

REM Find the JAR file
set JAR_PATH=javalin-benchmark\target\benchmarks-%BUILD_VERSION%.jar
if not exist "%JAR_PATH%" set JAR_PATH=javalin-benchmark\target\benchmarks.jar

if not exist "%JAR_PATH%" (
    echo [%BUILD_VERSION%] ERROR: Benchmark JAR not found at %JAR_PATH%
    exit /b 1
)

REM Run the benchmark
java -jar "%JAR_PATH%" "%BENCHMARK_PATTERN%" -wi %WARMUP_ITERATIONS% -i %MEASUREMENT_ITERATIONS% -f %FORKS% -rf json -rff "%OUTPUT_DIR%\results-%BUILD_VERSION%.json"
if errorlevel 1 (
    echo [%BUILD_VERSION%] ERROR: Benchmark execution failed
    exit /b 1
)

echo [%BUILD_VERSION%] Benchmarks complete
echo [%BUILD_VERSION%] Results: %OUTPUT_DIR%\results-%BUILD_VERSION%.json
goto :eof

:usage
echo Usage: %~nx0 -v1 ^<version1^> -v2 ^<version2^> [options]
echo.
echo Required arguments:
echo   -v1 ^<version^>    First Javalin version to benchmark (e.g., 6.3.0)
echo   -v2 ^<version^>    Second Javalin version to benchmark (e.g., 7.0.0-SNAPSHOT)
echo.
echo Optional arguments:
echo   -b ^<pattern^>     Benchmark pattern (regex, default: .*)
echo   -w ^<iterations^>  Warmup iterations (default: 3)
echo   -m ^<iterations^>  Measurement iterations (default: 5)
echo   -f ^<forks^>       Number of forks (default: 1)
echo   -o ^<dir^>         Output directory (default: benchmark-results)
echo   -h               Show this help message
echo.
echo Examples:
echo   REM Compare current version with 6.3.0
echo   %~nx0 -v1 6.3.0 -v2 7.0.0-SNAPSHOT
echo.
echo   REM Compare only routing benchmarks
echo   %~nx0 -v1 6.3.0 -v2 7.0.0-SNAPSHOT -b ".*Routing.*"
echo.
echo   REM Quick comparison with fewer iterations
echo   %~nx0 -v1 6.3.0 -v2 7.0.0-SNAPSHOT -w 1 -m 2
exit /b 1

