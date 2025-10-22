#!/bin/bash

# Javalin Version Comparison Script
# Compares performance between two Javalin versions

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
VERSION_1=""
VERSION_2=""
BENCHMARK_PATTERN=".*"
WARMUP_ITERATIONS=3
MEASUREMENT_ITERATIONS=5
FORKS=1
OUTPUT_DIR="benchmark-results"

# Function to print usage
usage() {
    echo "Usage: $0 -v1 <version1> -v2 <version2> [options]"
    echo ""
    echo "Required arguments:"
    echo "  -v1 <version>    First Javalin version to benchmark (e.g., 6.3.0)"
    echo "  -v2 <version>    Second Javalin version to benchmark (e.g., 7.0.0-SNAPSHOT)"
    echo ""
    echo "Optional arguments:"
    echo "  -b <pattern>     Benchmark pattern (regex, default: .*)"
    echo "  -w <iterations>  Warmup iterations (default: 3)"
    echo "  -m <iterations>  Measurement iterations (default: 5)"
    echo "  -f <forks>       Number of forks (default: 1)"
    echo "  -o <dir>         Output directory (default: benchmark-results)"
    echo "  -h               Show this help message"
    echo ""
    echo "Examples:"
    echo "  # Compare current version with 6.3.0"
    echo "  $0 -v1 6.3.0 -v2 7.0.0-SNAPSHOT"
    echo ""
    echo "  # Compare only routing benchmarks"
    echo "  $0 -v1 6.3.0 -v2 7.0.0-SNAPSHOT -b '.*Routing.*'"
    echo ""
    echo "  # Quick comparison with fewer iterations"
    echo "  $0 -v1 6.3.0 -v2 7.0.0-SNAPSHOT -w 1 -m 2"
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v1)
            VERSION_1="$2"
            shift 2
            ;;
        -v2)
            VERSION_2="$2"
            shift 2
            ;;
        -b)
            BENCHMARK_PATTERN="$2"
            shift 2
            ;;
        -w)
            WARMUP_ITERATIONS="$2"
            shift 2
            ;;
        -m)
            MEASUREMENT_ITERATIONS="$2"
            shift 2
            ;;
        -f)
            FORKS="$2"
            shift 2
            ;;
        -o)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            ;;
    esac
done

# Validate required arguments
if [ -z "$VERSION_1" ] || [ -z "$VERSION_2" ]; then
    echo -e "${RED}Error: Both -v1 and -v2 are required${NC}"
    usage
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Javalin Performance Comparison${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Version 1: ${GREEN}$VERSION_1${NC}"
echo -e "Version 2: ${GREEN}$VERSION_2${NC}"
echo -e "Benchmark pattern: ${YELLOW}$BENCHMARK_PATTERN${NC}"
echo -e "Warmup iterations: $WARMUP_ITERATIONS"
echo -e "Measurement iterations: $MEASUREMENT_ITERATIONS"
echo -e "Forks: $FORKS"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Clean the benchmark module once before building
echo ""
echo "Cleaning benchmark module..."
mvn clean -pl javalin-benchmark -q

# Function to build benchmark JAR for a specific version
build_benchmark() {
    local version=$1
    local profile_name="javalin-$version"

    echo -e "${YELLOW}[$version] Building benchmark JAR...${NC}"

    # Check if there's a predefined profile for this version
    if mvn help:all-profiles -pl javalin-benchmark 2>/dev/null | grep -q "$profile_name"; then
        mvn package -pl javalin-benchmark -P "$profile_name" -DskipTests -q
        if [ $? -ne 0 ]; then
            echo -e "${RED}[$version] ERROR: Build failed${NC}"
            return 1
        fi
    else
        # Use custom version
        echo -e "${YELLOW}[$version] Using custom version profile...${NC}"
        mvn package -pl javalin-benchmark -Djavalin.custom.version="$version" -DskipTests -q
        if [ $? -ne 0 ]; then
            echo -e "${RED}[$version] ERROR: Build failed${NC}"
            return 1
        fi
    fi

    echo -e "${GREEN}[$version] Build complete${NC}"
    return 0
}

# Function to run benchmarks for a specific version
run_benchmark() {
    local version=$1
    local jar_name="benchmarks-$version.jar"
    local output_file="$OUTPUT_DIR/results-$version.json"

    echo -e "${YELLOW}[$version] Running benchmarks...${NC}"

    # Find the JAR file in javalin-benchmark/target
    local jar_path="javalin-benchmark/target/$jar_name"
    if [ ! -f "$jar_path" ]; then
        # Try default name
        jar_path="javalin-benchmark/target/benchmarks.jar"
    fi

    if [ ! -f "$jar_path" ]; then
        echo -e "${RED}[$version] ERROR: Benchmark JAR not found at $jar_path${NC}"
        return 1
    fi

    # Run the benchmark
    java -jar "$jar_path" \
        "$BENCHMARK_PATTERN" \
        -wi "$WARMUP_ITERATIONS" \
        -i "$MEASUREMENT_ITERATIONS" \
        -f "$FORKS" \
        -rf json \
        -rff "$output_file"

    if [ $? -ne 0 ]; then
        echo -e "${RED}[$version] ERROR: Benchmark execution failed${NC}"
        return 1
    fi

    echo -e "${GREEN}[$version] Benchmarks complete${NC}"
    echo -e "${GREEN}[$version] Results: $output_file${NC}"
    return 0
}

# Build and run benchmarks for version 1
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Building and running: Javalin $VERSION_1${NC}"
echo -e "${BLUE}========================================${NC}"
if ! build_benchmark "$VERSION_1"; then
    echo -e "${RED}Aborting comparison due to build failure${NC}"
    exit 1
fi
if ! run_benchmark "$VERSION_1"; then
    echo -e "${RED}Aborting comparison due to benchmark failure${NC}"
    exit 1
fi

# Build and run benchmarks for version 2
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Building and running: Javalin $VERSION_2${NC}"
echo -e "${BLUE}========================================${NC}"
if ! build_benchmark "$VERSION_2"; then
    echo -e "${RED}Aborting comparison due to build failure${NC}"
    exit 1
fi
if ! run_benchmark "$VERSION_2"; then
    echo -e "${RED}Aborting comparison due to benchmark failure${NC}"
    exit 1
fi

# Show results summary
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Comparison Results${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Parse and display results using jq if available, otherwise use Python
if command -v jq &> /dev/null; then
    # Use jq for parsing
    echo "===================================================================================================="
    printf "%-60s %15s %15s %10s\n" "Benchmark" "$VERSION_1" "$VERSION_2" "Change"
    echo "===================================================================================================="

    # Read both JSON files
    v1_file="$OUTPUT_DIR/results-$VERSION_1.json"
    v2_file="$OUTPUT_DIR/results-$VERSION_2.json"

    # Get number of benchmarks
    count=$(jq 'length' "$v1_file")

    # Iterate through benchmarks
    for i in $(seq 0 $((count - 1))); do
        name=$(jq -r ".[$i].benchmark" "$v1_file" | sed 's/io.javalin.benchmark.//')
        score1=$(jq -r ".[$i].primaryMetric.score" "$v1_file")
        score2=$(jq -r ".[$i].primaryMetric.score" "$v2_file")
        unit=$(jq -r ".[$i].primaryMetric.scoreUnit" "$v1_file")

        # Calculate percentage change
        change=$(echo "scale=2; (($score2 - $score1) / $score1) * 100" | bc)

        # Format change with + or - sign
        if (( $(echo "$change > 0" | bc -l) )); then
            change_str="+${change}%"
        elif (( $(echo "$change < 0" | bc -l) )); then
            change_str="${change}%"
        else
            change_str="no change"
        fi

        # Round scores for display
        score1_rounded=$(printf "%.3f" "$score1")
        score2_rounded=$(printf "%.3f" "$score2")

        printf "%-60s %12s %-2s %12s %-2s %10s\n" "$name" "$score1_rounded" "$unit" "$score2_rounded" "$unit" "$change_str"
    done

    echo "===================================================================================================="
else
    # Fallback: just show file locations
    echo -e "${YELLOW}Note: Install 'jq' for formatted comparison table${NC}"
    echo ""
    echo -e "${GREEN}Javalin $VERSION_1 vs $VERSION_2${NC}"
    echo ""
    echo "Results saved to:"
    echo "  $OUTPUT_DIR/results-$VERSION_1.json"
    echo "  $OUTPUT_DIR/results-$VERSION_2.json"
fi

echo ""
echo "Results also saved to:"
echo "  $OUTPUT_DIR/results-$VERSION_1.json"
echo "  $OUTPUT_DIR/results-$VERSION_2.json"
echo ""
echo "For visual comparison, upload JSON files to: https://jmh.morethan.io/"
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Comparison Complete!${NC}"
echo -e "${GREEN}========================================${NC}"

