from pathlib import Path

# ==========================
# Configuration
# ==========================

ROOT = Path("../")

EXCLUDED_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    "build",
    ".kotlin",
    "captures",
    ".externalNativeBuild",
    ".cxx",
    "generated",
    "__pycache__",
    "res",
    "dicts",
    "og dicks backup",
    "oversize-files.txt"
}

EXCLUDED_FILES = {
    ".DS_Store",
}

# ==========================
# Tree Generator
# ==========================

lines = []


def build_tree(directory: Path, prefix=""):
    entries = sorted(
        [
            p
            for p in directory.iterdir()
            if p.name not in EXCLUDED_DIRS
            and p.name not in EXCLUDED_FILES
        ],
        key=lambda p: (p.is_file(), p.name.lower()),
    )

    for i, entry in enumerate(entries):
        is_last = i == len(entries) - 1

        connector = "└── " if is_last else "├── "

        lines.append(prefix + connector + entry.name)

        if entry.is_dir():
            extension = "    " if is_last else "│   "
            build_tree(entry, prefix + extension)


# Root title
lines.append(ROOT.resolve().name)

build_tree(ROOT)

output = "\n".join(lines)

print(output)

with open("folder_structure.txt", "w", encoding="utf-8") as f:
    f.write(output)

print("\nSaved to folder_structure.txt")
