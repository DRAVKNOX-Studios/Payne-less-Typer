#!/usr/bin/env python3
"""
build_dict.py  --  offline builder for SwiftLite binary dictionary

Usage (from repo root):
    python tools/build_dict.py \
        --dicts  tools/dicts \
        --assets app/src/main/assets/dicts \
        --out    app/src/main/assets/dicts/en.dict

  --dicts   directory containing the large build-time .txt wordlists
  --assets  directory containing the small runtime grammar .txt files
  --out     path to write the binary .dict file

Output binary layout:
    [4]  magic     0x53574454  ('SWDT')
    [4]  count     uint32
    [count * 4]  offsets  uint32  (byte offsets into DATA BLOCK)
    [DATA BLOCK]
        per entry:  1 byte category | 1 byte word_len | N bytes UTF-8 word
"""

import argparse
import os
import struct
import sys

# (relative_path, category, source)  source: 'dicts' = tools/dicts, 'assets' = assets/dicts
DICT_FILES = [
    ("articles.txt",                1, "assets"),
    ("auxiliary_verbs.txt",         1, "assets"),
    ("conjunctions.txt",            1, "assets"),
    ("prepositions.txt",            1, "assets"),
    ("pronouns.txt",                1, "assets"),
    ("contractions.txt",            0, "assets"),
    ("core.txt",                    0, "dicts"),
    ("common.txt",                  2, "dicts"),
    ("uncommon.txt",                3, "dicts"),
    ("rare.txt",                    4, "dicts"),
    ("extra.txt",                   5, "dicts"),
    ("slang_wordlist.txt",          6, "dicts"),
    ("brainrot_wordlist.txt",       7, "dicts"),
    ("dev_wordlist.txt",            8, "dicts"),
    ("atlas_wordlist.txt",          9, "dicts"),
    ("named_entities_wordlist.txt", 10, "dicts"),
]

LOWERCASE_CATS = {0, 1, 2, 3, 4, 5, 6, 7}
MAGIC = 0x53574454


def read_words(dicts_dir, assets_dir):
    seen = set()
    entries = []
    for filename, cat, source in DICT_FILES:
        full = os.path.join(dicts_dir if source == "dicts" else assets_dir, filename)
        if not os.path.exists(full):
            print(f"  skipping missing: {full}", file=sys.stderr)
            continue
        count = 0
        with open(full, encoding="utf-8", errors="replace") as f:
            for line in f:
                w = line.strip()
                if not w or w.startswith("#"):
                    continue
                if cat in LOWERCASE_CATS:
                    w = w.lower()
                key = w.lower()
                if key in seen:
                    continue
                seen.add(key)
                encoded = w.encode("utf-8")
                if len(encoded) > 255:
                    continue
                entries.append((key, encoded, cat))
                count += 1
        print(f"  loaded {count:>7,}  [{source}] {filename}")
    return entries


def write_dict(entries, out_path):
    entries.sort(key=lambda e: e[0])
    count = len(entries)
    data_parts = []
    offsets = []
    pos = 0
    for _, encoded, cat in entries:
        offsets.append(pos)
        chunk = bytes([cat, len(encoded)]) + encoded
        data_parts.append(chunk)
        pos += len(chunk)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "wb") as f:
        f.write(struct.pack(">II", MAGIC, count))
        for off in offsets:
            f.write(struct.pack(">I", off))
        for chunk in data_parts:
            f.write(chunk)
    size_mb = os.path.getsize(out_path) / (1024 * 1024)
    print(f"\nWrote {count:,} entries -> {out_path}  ({size_mb:.2f} MB)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dicts",  required=True, help="Path to tools/dicts (build-time wordlists)")
    ap.add_argument("--assets", required=True, help="Path to assets/dicts (runtime grammar files)")
    ap.add_argument("--out",    required=True, help="Output .dict file path")
    args = ap.parse_args()
    print("Reading word lists...")
    entries = read_words(args.dicts, args.assets)
    print(f"Total unique entries: {len(entries):,}")
    print("Writing binary dict...")
    write_dict(entries, args.out)


if __name__ == "__main__":
    main()
