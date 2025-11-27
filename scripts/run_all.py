#!/usr/bin/env python3
import subprocess
import glob

for script in glob.glob("*.py"):
    if script != "run_all.py":
        subprocess.run(["python3", script], check=True)