from pathlib import Path
import shutil

DICTS_DIR = Path(__file__).parent / "dicts"

PRIORITY_FILES = [
    "articles.txt",
    "pronouns.txt",
    "prepositions.txt",
    "conjunctions.txt",
    "auxiliary_verbs.txt",
    "contractions.txt",
    "core.txt",
    "common.txt",
    "uncommon.txt",
    "rare.txt",
    "slang_wordlist.txt",
    "dev_wordlist.txt",
    "atlas_wordlist.txt",
    "named_entities_wordlist.txt",
    "brainrot_wordlist.txt",
    "extra.txt",
]


def load_words(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return {line.strip().lower() for line in f if line.strip()}
    except FileNotFoundError:
        print(f"Missing: {path.name}")
        return set()


def save_words(path, words):
    with open(path, "w", encoding="utf-8") as f:
        for word in sorted(words):
            f.write(word + "\n")


def main():
    claimed = set()
    print("\n=== Priority Deduplication ===\n")
    for filename in PRIORITY_FILES:
        path = DICTS_DIR / filename
        words = load_words(path)
        original = len(words)
        shutil.copy2(path, path.with_suffix(path.suffix + ".bak"))
        cleaned = words - claimed
        save_words(path, cleaned)
        claimed.update(cleaned)
        print(f"{filename}: {original:,} -> {len(cleaned):,} (removed {original - len(cleaned):,})")
    print("\n==============================")
    print(f"Total unique words: {len(claimed):,}")
    print("==============================\n")


if __name__ == "__main__":
    main()
