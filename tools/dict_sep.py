from pathlib import Path

DICTS_DIR = Path(__file__).parent / "dicts"

PREVIOUS_DICTS = [
    "dict1.txt",
    "dict2.txt",
    "dict3.txt",
    "dict4.txt",
]

NEW_DICT = "dict5.txt"

EXCLUDE_FILES = [
    "articles.txt",
    "pronouns.txt",
    "prepositions.txt",
    "conjunctions.txt",
    "auxiliary_verbs.txt",
    "core.txt",
    "common.txt",
    "uncommon.txt",
]

OUTPUT_FILE = DICTS_DIR / "rare.txt"


def load_words(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return {line.strip().lower() for line in f if line.strip()}
    except FileNotFoundError:
        print(f"Warning: {path} not found")
        return set()


def save_words(path, words):
    with open(path, "w", encoding="utf-8") as f:
        for word in sorted(words):
            f.write(word + "\n")


def main():
    previous = []
    for name in PREVIOUS_DICTS:
        words = load_words(DICTS_DIR / name)
        previous.append(words)
        print(f"{name}: {len(words)} words")

    new_words = load_words(DICTS_DIR / NEW_DICT)
    print(f"{NEW_DICT}: {len(new_words)} words")

    known = set()
    for name in EXCLUDE_FILES:
        known.update(load_words(DICTS_DIR / name))
    print(f"Known words: {len(known)}")

    discovered = set()
    for words in previous:
        overlap = words & new_words
        print(f"Pair overlap: {len(overlap)}")
        discovered |= overlap
    print(f"Total discovered: {len(discovered)}")

    new_tier = discovered - known
    print(f"New words for rare.txt: {len(new_tier)}")
    save_words(OUTPUT_FILE, new_tier)

    cleaned = new_words - discovered
    save_words(DICTS_DIR / NEW_DICT.replace(".txt", "_cleaned.txt"), cleaned)
    print(f"Remaining cleaned {NEW_DICT}: {len(cleaned)}")
    print("\nDone.")


if __name__ == "__main__":
    main()
