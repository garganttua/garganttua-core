#!/bin/bash
#
# Garganttua Script - Debug/Profiling Runner
#
# Usage:
#   ./garganttua-script-debug.sh [options] [script.gs] [args...]
#
# Options:
#   --gc          Enable GC logging
#   --jit         Enable JIT compilation logging
#   --class       Enable class loading logging
#   --safepoint   Enable safepoint logging
#   --timing      Enable internal timing (via logback-debug.xml)
#   --heap N      Set heap size (e.g., --heap 512m)
#   --profile     Enable all profiling options
#   --flight      Enable Java Flight Recorder
#   --all         Enable all debug options
#
# Examples:
#   ./garganttua-script-debug.sh --timing script.gs
#   ./garganttua-script-debug.sh --gc --jit script.gs
#   ./garganttua-script-debug.sh --profile script.gs
#   ./garganttua-script-debug.sh --flight script.gs  # Creates recording.jfr

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="${SCRIPT_DIR}/garganttua-script-debug.jar"

# Default JVM options
JVM_OPTS=""
HEAP_SIZE="256m"
ENABLE_TIMING=false
ENABLE_FLIGHT=false

# Parse options
while [[ $# -gt 0 ]]; do
    case "$1" in
        --gc)
            JVM_OPTS="$JVM_OPTS -Xlog:gc*:file=gc.log:time,uptime,level,tags"
            shift
            ;;
        --jit)
            JVM_OPTS="$JVM_OPTS -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining"
            shift
            ;;
        --class)
            JVM_OPTS="$JVM_OPTS -Xlog:class+load=info:file=classload.log:time"
            shift
            ;;
        --safepoint)
            JVM_OPTS="$JVM_OPTS -Xlog:safepoint*:file=safepoint.log:time,uptime"
            shift
            ;;
        --timing)
            ENABLE_TIMING=true
            shift
            ;;
        --heap)
            HEAP_SIZE="$2"
            shift 2
            ;;
        --profile)
            JVM_OPTS="$JVM_OPTS -Xlog:gc*:file=gc.log:time,uptime,level,tags"
            JVM_OPTS="$JVM_OPTS -XX:+PrintCompilation"
            JVM_OPTS="$JVM_OPTS -Xlog:class+load=info:file=classload.log:time"
            ENABLE_TIMING=true
            shift
            ;;
        --flight)
            ENABLE_FLIGHT=true
            JVM_OPTS="$JVM_OPTS -XX:+FlightRecorder"
            JVM_OPTS="$JVM_OPTS -XX:StartFlightRecording=duration=0s,filename=recording.jfr,settings=profile"
            shift
            ;;
        --all)
            JVM_OPTS="$JVM_OPTS -Xlog:gc*:file=gc.log:time,uptime,level,tags"
            JVM_OPTS="$JVM_OPTS -XX:+PrintCompilation"
            JVM_OPTS="$JVM_OPTS -Xlog:class+load=info:file=classload.log:time"
            JVM_OPTS="$JVM_OPTS -Xlog:safepoint*:file=safepoint.log:time,uptime"
            JVM_OPTS="$JVM_OPTS -XX:+FlightRecorder"
            JVM_OPTS="$JVM_OPTS -XX:StartFlightRecording=duration=0s,filename=recording.jfr,settings=profile"
            ENABLE_TIMING=true
            ENABLE_FLIGHT=true
            shift
            ;;
        --help|-h)
            head -30 "$0" | tail -28
            exit 0
            ;;
        *)
            # End of options, rest are script arguments
            break
            ;;
    esac
done

# Set heap size
JVM_OPTS="-Xms${HEAP_SIZE} -Xmx${HEAP_SIZE} $JVM_OPTS"

# Enable timing via debug logback config
if [ "$ENABLE_TIMING" = true ]; then
    JVM_OPTS="$JVM_OPTS -Dlogback.configurationFile=logback-debug.xml"
fi

# Print configuration
echo "========================================"
echo "Garganttua Script - Debug Mode"
echo "========================================"
echo "JAR: $JAR_FILE"
echo "Heap: $HEAP_SIZE"
echo "JVM Options: $JVM_OPTS"
echo "Arguments: $@"
echo "========================================"
echo ""

# Record start time
START_TIME=$(date +%s%N)

# Run the JAR
java $JVM_OPTS -jar "$JAR_FILE" "$@"
EXIT_CODE=$?

# Record end time and calculate duration
END_TIME=$(date +%s%N)
DURATION_NS=$((END_TIME - START_TIME))
DURATION_MS=$((DURATION_NS / 1000000))
DURATION_S=$(echo "scale=3; $DURATION_MS / 1000" | bc)

echo ""
echo "========================================"
echo "Execution Summary"
echo "========================================"
echo "Exit code: $EXIT_CODE"
echo "Total time: ${DURATION_S}s (${DURATION_MS}ms)"

if [ "$ENABLE_FLIGHT" = true ] && [ -f "recording.jfr" ]; then
    echo "Flight recording: recording.jfr"
    echo "  View with: jfr print recording.jfr"
    echo "  Or open in JDK Mission Control"
fi

if [ -f "gc.log" ]; then
    echo "GC log: gc.log"
    GC_COUNT=$(grep -c "GC(" gc.log 2>/dev/null || echo "0")
    echo "  GC events: $GC_COUNT"
fi

if [ -f "classload.log" ]; then
    echo "Class loading log: classload.log"
    CLASS_COUNT=$(wc -l < classload.log 2>/dev/null || echo "0")
    echo "  Classes loaded: $CLASS_COUNT"
fi

echo "========================================"

exit $EXIT_CODE
