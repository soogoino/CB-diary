#!/usr/bin/env python3
import xml.etree.ElementTree as ET
import subprocess
import shutil
import os
import sys
import tempfile

REPO = os.getcwd()
BACKUP = os.path.join(REPO, "strings.xml.backup")
TARGET = os.path.join(REPO, "app/src/main/res/values/strings.xml")
GRADLE_CMD = ["./gradlew", ":app:mergeDebugResources", "--no-daemon", "--stacktrace"]
TIMEOUT = 600

if not os.path.isfile(BACKUP):
    print(f"Backup file not found: {BACKUP}")
    sys.exit(2)

print("Parsing backup...", BACKUP)
try:
    tree = ET.parse(BACKUP)
    root = tree.getroot()
except ET.ParseError as e:
    print("Failed to parse backup XML:", e)
    sys.exit(2)

children = list(root)
count = len(children)
print(f"Found {count} top-level child elements under <resources>.")
if count == 0:
    print("No child elements to bisect.")
    sys.exit(0)

# backup current target to a safe .xml filename outside resources conflict
PRE_BISect_DIR = os.path.join(REPO, ".bisect_pre_bak")
os.makedirs(PRE_BISect_DIR, exist_ok=True)
PRE_BISect_BACKUP = os.path.join(PRE_BISect_DIR, "strings.xml.bak")
if os.path.isfile(TARGET):
    shutil.copy2(TARGET, PRE_BISect_BACKUP)
    print("Backed up current TARGET to", PRE_BISect_BACKUP)

# helper to write selected indices into TARGET

def write_selected(indices):
    resources = ET.Element('resources')
    for i in indices:
        # append a deep copy to avoid multiple-parent issues
        resources.append(children[i])
    tree2 = ET.ElementTree(resources)
    tree2.write(TARGET, encoding='utf-8', xml_declaration=True)

# helper to run gradle merge resources

def run_gradle():
    print("Running:", ' '.join(GRADLE_CMD))
    try:
        p = subprocess.run(GRADLE_CMD, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=TIMEOUT)
        out = p.stdout.decode('utf-8', errors='replace')
        print(out.splitlines()[-20:])
        return p.returncode, out
    except subprocess.TimeoutExpired:
        print("Gradle timed out after", TIMEOUT, "seconds")
        return 124, "timeout"

# sanity check: write full backup contents and verify it fails (or not)
print("Writing full backup to target for sanity check...")
shutil.copy2(BACKUP, TARGET)
rc_full, out_full = run_gradle()
if rc_full == 0:
    print("Full backup compiled successfully. No failing resource detected by this script.")
    # restore original target backup if present
    if os.path.isfile(TARGET + ".pre-bisect.bak"):
        shutil.move(TARGET + ".pre-bisect.bak", TARGET)
    sys.exit(0)

print("Full build failed as expected (or resources are malformed). Starting bisect...")

indices = list(range(count))

# recursive bisect over indices

def bisect_range(idxs):
    if len(idxs) == 0:
        return None
    if len(idxs) == 1:
        write_selected(idxs)
        rc, out = run_gradle()
        if rc != 0:
            return idxs[0]
        return None
    mid = len(idxs) // 2
    left = idxs[:mid]
    right = idxs[mid:]
    print(f"Testing left half size={len(left)}")
    write_selected(left)
    rc, out = run_gradle()
    if rc != 0:
        return bisect_range(left)
    print("Left half ok; testing right half")
    write_selected(right)
    rc2, out2 = run_gradle()
    if rc2 != 0:
        return bisect_range(right)
    # If neither half alone fails, try combination approach: test interleaved singles
    print("Neither half alone failed; trying single-element checks")
    for i in idxs:
        write_selected([i])
        rci, outi = run_gradle()
        if rci != 0:
            return i
    return None

bad_index = bisect_range(indices)

if bad_index is None:
    print("Could not isolate a single failing element. The failure may involve combinations or non-XML issues.")
    # restore original
    if os.path.isfile(TARGET + ".pre-bisect.bak"):
        shutil.move(TARGET + ".pre-bisect.bak", TARGET)
    sys.exit(3)

bad_elem = children[bad_index]
name = bad_elem.get('name')
print(f"Found failing element at index {bad_index} (name={name})")

# write bad element to a file for inspection
badfile = os.path.join(REPO, f".bisect_bad_element_{bad_index}.xml")
wrapper = ET.Element('resources')
wrapper.append(bad_elem)
ET.ElementTree(wrapper).write(badfile, encoding='utf-8', xml_declaration=True)
print("Wrote failing element to:", badfile)

# restore original TARGET backup (move pre-bisect back)
if os.path.isfile(PRE_BISect_BACKUP):
    shutil.move(PRE_BISect_BACKUP, TARGET)
    print("Restored original TARGET from pre-bisect backup.")

print("Bisect complete.")
print(f"Offending index: {bad_index}, name: {name}")
sys.exit(0)
