#!/usr/bin/env python3
"""Append Gradle section to 004-maven-and-gradle.md by replacing placeholder lines."""

import os

BASE = "/Users/abhishekghosh/Desktop/personal-projects/spring-boot-helper"
TARGET = os.path.join(BASE, "docs/spring-introduction/004-maven-and-gradle.md")

GRADLE_CONTENT = r"""


"""

# Read the file and replace the placeholder section
with open(TARGET, 'r', encoding='utf-8') as f:
    content = f.read()

# Find the placeholder start marker
PLACEHOLDER_START = "\n\n- What is Gradle?"
idx = content.find(PLACEHOLDER_START)
if idx == -1:
    print("ERROR: Could not find placeholder start marker")
    exit(1)

new_content = content[:idx] + GRADLE_CONTENT

with open(TARGET, 'w', encoding='utf-8') as f:
    f.write(new_content)

print(f"Written {len(new_content)} chars to {TARGET}")
