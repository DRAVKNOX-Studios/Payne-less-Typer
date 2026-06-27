from pathlib import Path

DICTS_DIR = Path(__file__).parent / "dicts"

SOURCE_FILES = [
    "dict1_cleaned.txt",
    "dict2_cleaned.txt",
    "dict3_cleaned.txt",
    "dict4_cleaned.txt",
    "dict5_cleaned.txt",
]

OUTPUT_FILE = DICTS_DIR / "extra.txt"


def load_words(path):
    with open(path, "r", encoding="utf-8") as f:
        return {line.strip().lower() for line in f if line.strip()}


def save_words(path, words):
    with open(path, "w", encoding="utf-8") as f:
        for word in sorted(words):
            f.write(word + "\n")


def main():
    all_words = set()
    for filename in SOURCE_FILES:
        path = DICTS_DIR / filename
        words = load_words(path)
        print(f"{filename}: {len(words)} words")
        all_words.update(words)
    save_words(OUTPUT_FILE, all_words)
    print(f"\nextra.txt: {len(all_words)} unique words")


if __name__ == "__main__":
    main()
