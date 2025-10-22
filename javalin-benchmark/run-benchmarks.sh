#!/bin/bash

# Javalin Benchmark Runner Script
# This script provides convenient ways to run benchmarks with common configurations

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
BENCHMARK_JAR="target/benchmarks.jar"
OUTPUT_DIR="benchmark-results"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if JAR exists
check_jar() {
    if [ ! -f "$BENCHMARK_JAR" ]; then
        print_error "Benchmark JAR not found at $BENCHMARK_JAR"
        print_info "Building benchmark JAR..."
        mvn clean package
    fi
}

# Function to create output directory
setup_output_dir() {
    mkdir -p "$OUTPUT_DIR"
}

# Show usage
usage() {
    cat << EOF
Usage: $0 [COMMAND] [OPTIONS]

Commands:
    all             Run all benchmarks (default)
    routing         Run routing benchmarks only
    static          Run static file benchmarks only
    context         Run context methods benchmarks only
    e2e             Run end-to-end benchmarks only
    quick           Run quick benchmarks (fewer iterations)
    compare         Compare two benchmark result files
    
Options:
    -h, --help      Show this help message
    -o, --output    Output directory (default: benchmark-results)
    
Examples:
    $0 all                          # Run all benchmarks
    $0 routing                      # Run routing benchmarks
    $0 quick                        # Quick run for development
    $0 compare results1.json results2.json

EOF
}

# Run all benchmarks
run_all() {
    print_info "Running all benchmarks..."
    setup_output_dir
    local output_file="$OUTPUT_DIR/all-benchmarks-$TIMESTAMP.json"
    
    java -jar "$BENCHMARK_JAR" \
        -rf json \
        -rff "$output_file"
    
    print_info "Results saved to: $output_file"
}

# Run routing benchmarks
run_routing() {
    print_info "Running routing benchmarks..."
    setup_output_dir
    local output_file="$OUTPUT_DIR/routing-$TIMESTAMP.json"
    
    java -jar "$BENCHMARK_JAR" RoutingBenchmark \
        -rf json \
        -rff "$output_file"
    
    print_info "Results saved to: $output_file"
}

# Run static file benchmarks
run_static() {
    print_info "Running static file benchmarks..."
    setup_output_dir
    local output_file="$OUTPUT_DIR/static-$TIMESTAMP.json"
    
    java -jar "$BENCHMARK_JAR" StaticFilesBenchmark \
        -rf json \
        -rff "$output_file"
    
    print_info "Results saved to: $output_file"
}

# Run context methods benchmarks
run_context() {
    print_info "Running context methods benchmarks..."
    setup_output_dir
    local output_file="$OUTPUT_DIR/context-$TIMESTAMP.json"
    
    java -jar "$BENCHMARK_JAR" ContextMethodsBenchmark \
        -rf json \
        -rff "$output_file"
    
    print_info "Results saved to: $output_file"
}

# Run end-to-end benchmarks
run_e2e() {
    print_info "Running end-to-end benchmarks..."
    setup_output_dir
    local output_file="$OUTPUT_DIR/e2e-$TIMESTAMP.json"
    
    java -jar "$BENCHMARK_JAR" EndToEndBenchmark \
        -rf json \
        -rff "$output_file"
    
    print_info "Results saved to: $output_file"
}

# Run quick benchmarks (for development)
run_quick() {
    print_info "Running quick benchmarks (reduced iterations)..."
    setup_output_dir
    local output_file="$OUTPUT_DIR/quick-$TIMESTAMP.json"
    
    java -jar "$BENCHMARK_JAR" \
        -wi 1 \
        -i 2 \
        -f 1 \
        -rf json \
        -rff "$output_file"
    
    print_info "Results saved to: $output_file"
}

# Compare two benchmark results
compare_results() {
    if [ $# -lt 2 ]; then
        print_error "Compare requires two result files"
        echo "Usage: $0 compare <baseline.json> <current.json>"
        exit 1
    fi
    
    local baseline="$1"
    local current="$2"
    
    if [ ! -f "$baseline" ]; then
        print_error "Baseline file not found: $baseline"
        exit 1
    fi
    
    if [ ! -f "$current" ]; then
        print_error "Current file not found: $current"
        exit 1
    fi
    
    print_info "Comparing benchmarks:"
    print_info "  Baseline: $baseline"
    print_info "  Current:  $current"
    print_warn "Note: Detailed comparison requires custom tooling"
    print_info "Consider using: https://github.com/jzillmann/jmh-visualizer"
}

# Main script logic
main() {
    check_jar
    
    case "${1:-all}" in
        all)
            run_all
            ;;
        routing)
            run_routing
            ;;
        static)
            run_static
            ;;
        context)
            run_context
            ;;
        e2e)
            run_e2e
            ;;
        quick)
            run_quick
            ;;
        compare)
            shift
            compare_results "$@"
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            print_error "Unknown command: $1"
            usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"

