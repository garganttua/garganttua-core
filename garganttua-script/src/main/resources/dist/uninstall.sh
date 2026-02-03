#!/bin/bash
#
# Garganttua Script Engine - Uninstaller
#

set -e

PREFIX="/usr/local"

echo "Uninstalling Garganttua Script Engine from $PREFIX..."

# Remove files
rm -f "$PREFIX/bin/garganttua-script"
rm -f "$PREFIX/bin/gs"
rm -rf "$PREFIX/lib/garganttua-script"
rm -rf "$PREFIX/etc/garganttua-script"

echo "Uninstallation complete."
