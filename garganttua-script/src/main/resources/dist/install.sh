#!/bin/bash
#
# Garganttua Script Engine - Linux Installer
#
# Usage: sudo ./install.sh [--prefix=/usr/local]
#

set -e

PREFIX="/usr/local"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MIN_JAVA_VERSION=21

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --prefix=*)
            PREFIX="${1#*=}"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [--prefix=/usr/local]"
            echo ""
            echo "Options:"
            echo "  --prefix=PATH    Installation prefix (default: /usr/local)"
            echo "  --help, -h       Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Check if Java is installed
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo ""
    echo "ERROR: Java is not installed or not in PATH"
    echo ""
    echo "Garganttua Script requires Java $MIN_JAVA_VERSION or later."
    echo "Please install a JRE/JDK before running this installer."
    echo ""
    echo "Installation options:"
    echo "  - Ubuntu/Debian: sudo apt install openjdk-$MIN_JAVA_VERSION-jre"
    echo "  - Fedora/RHEL:   sudo dnf install java-$MIN_JAVA_VERSION-openjdk"
    echo "  - Arch Linux:    sudo pacman -S jre$MIN_JAVA_VERSION-openjdk"
    echo "  - Or download from: https://adoptium.net/"
    echo ""
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ -z "$JAVA_VERSION" ]; then
    # Try alternative parsing for newer Java versions
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | sed 's/.*version "\([0-9]*\).*/\1/')
fi

if [ "$JAVA_VERSION" -lt "$MIN_JAVA_VERSION" ] 2>/dev/null; then
    echo ""
    echo "ERROR: Java $MIN_JAVA_VERSION or later is required"
    echo ""
    echo "Current Java version: $JAVA_VERSION"
    echo "Required Java version: $MIN_JAVA_VERSION+"
    echo ""
    echo "Please upgrade your Java installation before running this installer."
    echo ""
    exit 1
fi

echo "Found Java $JAVA_VERSION - OK"
echo ""
echo "Installing Garganttua Script Engine to $PREFIX..."

# Create directories
mkdir -p "$PREFIX/lib/garganttua-script"
mkdir -p "$PREFIX/bin"
mkdir -p "$PREFIX/etc/garganttua-script"

# Copy all JAR files
echo "Copying JAR files..."
cp "$SCRIPT_DIR/lib/"*.jar "$PREFIX/lib/garganttua-script/"

# Copy launcher scripts
echo "Installing launcher scripts..."
cp "$SCRIPT_DIR/bin/garganttua-script" "$PREFIX/bin/"
cp "$SCRIPT_DIR/bin/gs" "$PREFIX/bin/"

# Copy configuration
cp "$SCRIPT_DIR/conf/logback.xml" "$PREFIX/etc/garganttua-script/"

# Update the wrapper scripts with correct paths
sed -i "s|INSTALL_DIR=.*|INSTALL_DIR=\"$PREFIX\"|g" "$PREFIX/bin/garganttua-script"
sed -i "s|INSTALL_DIR=.*|INSTALL_DIR=\"$PREFIX\"|g" "$PREFIX/bin/gs"

# Make wrappers executable
chmod +x "$PREFIX/bin/garganttua-script"
chmod +x "$PREFIX/bin/gs"

echo ""
echo "Installation complete!"
echo ""
echo "Usage:"
echo "  garganttua-script script.gs [args...]"
echo "  gs script.gs [args...]              # Short alias"
echo ""
echo "You can also create executable scripts with shebang:"
echo "  #!/usr/bin/env garganttua-script"
echo "  or"
echo "  #!/usr/bin/env gs"
echo ""
echo "To uninstall, run: $PREFIX/lib/garganttua-script/uninstall.sh"

# Copy uninstall script
cp "$SCRIPT_DIR/uninstall.sh" "$PREFIX/lib/garganttua-script/"
sed -i "s|PREFIX=.*|PREFIX=\"$PREFIX\"|g" "$PREFIX/lib/garganttua-script/uninstall.sh"
chmod +x "$PREFIX/lib/garganttua-script/uninstall.sh"
