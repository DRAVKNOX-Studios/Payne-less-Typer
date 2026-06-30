#!/usr/bin/env python3
import json
import urllib.request
import re
from pathlib import Path

# Paths
ROOT = Path(__file__).parent.parent
DATA_PATH = ROOT / "app/src/main/assets/emoji_data.json"
SHORTCODES_PATH = ROOT / "app/src/main/assets/emoji_shortcodes.json"

# Source
UNICODE_URL = "https://www.unicode.org/Public/emoji/16.0/emoji-test.txt"

def clean_name(name):
    """Convert 'Grinning Face with Big Eyes' to 'grinning_face'"""
    name = name.lower()
    name = re.sub(r'[:\(\)\.,]', '', name)
    name = name.replace(" ", "_").replace("-", "_")
    return name

def fetch_unicode_data(url):
    print(f"Fetching: {url}")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=15) as response:
        return response.read().decode('utf-8')

def sync():
    try:
        content = fetch_unicode_data(UNICODE_URL)
    except Exception as e:
        print(f"Error fetching Unicode data: {e}")
        return

    categories = []
    current_category = []
    shortcodes = {}

    # Regex to catch group headers and emoji lines
    group_pattern = re.compile(r"^# group: (.*)$")
    # 1F600 ; fully-qualified # 😀 E1.0 grinning face
    emoji_pattern = re.compile(r"^.*?; fully-qualified\s*#\s*(\S+)\s+E\d+\.\d+\s+(.*)$")

    for line in content.splitlines():
        line = line.strip()

        # Check for new group
        group_match = group_pattern.match(line)
        if group_match:
            if current_category:
                categories.append(current_category)
            current_category = []
            continue

        # Check for emoji
        emoji_match = emoji_pattern.match(line)
        if emoji_match:
            emoji_char = emoji_match.group(1)
            full_name = emoji_match.group(2)

            # Skip skin tone variations in the main categories list to keep it concise
            # (Users usually access these via long-press)
            if "skin tone" in full_name:
                continue

            current_category.append(emoji_char)

            # Generate shortcode
            name = clean_name(full_name)
            shortcodes[name] = emoji_char

            # Add alias for first word
            first_word = name.split("_")[0]
            if len(first_word) > 3 and first_word not in shortcodes:
                shortcodes[first_word] = emoji_char

    # Add the last category
    if current_category:
        categories.append(current_category)

    # UI LIMITATION: EmojiPanel has 10 icons.
    # 1 is for Recents. We need exactly 9 categories.
    # Unicode has 10 groups. We merge 'Symbols' and 'Flags' (the last two).
    if len(categories) > 9:
        merged = []
        for i in range(8):
            merged.append(categories[i])

        # Merge the rest
        last_group = []
        for i in range(8, len(categories)):
            last_group.extend(categories[i])
        merged.append(last_group)
        categories = merged

    # 1. Update emoji_data.json
    print(f"Saving {len(categories)} categories to {DATA_PATH.name}...")
    emoji_data = {
        "version": "16.0",
        "source": UNICODE_URL,
        "categories": categories
    }
    with open(DATA_PATH, "w", encoding="utf-8") as f:
        json.dump(emoji_data, f, ensure_ascii=False, separators=(',', ':'))

    # 2. Update emoji_shortcodes.json
    print(f"Saving {len(shortcodes)} shortcodes to {SHORTCODES_PATH.name}...")
    sorted_shortcodes = dict(sorted(shortcodes.items()))
    with open(SHORTCODES_PATH, "w", encoding="utf-8") as f:
        json.dump(sorted_shortcodes, f, ensure_ascii=False, indent=2)

    print("Sync complete!")

if __name__ == "__main__":
    sync()
