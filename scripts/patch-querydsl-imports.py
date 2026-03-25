# Post-process QueryDSL Q-types: IDE often fails on `import com.querydsl.core.types.dsl.*;`
# for EntityPathBase resolution. Add explicit import after compile (process-classes).
import pathlib
import sys

NEEDLE = "import com.querydsl.core.types.dsl.*;"
EXTRA = "\nimport com.querydsl.core.types.dsl.EntityPathBase;"


def main() -> None:
    root = pathlib.Path(sys.argv[1])
    if not root.is_dir():
        return
    for path in root.rglob("Q*.java"):
        try:
            text = path.read_text(encoding="utf-8")
        except OSError:
            continue
        if "extends EntityPathBase" not in text:
            continue
        if "import com.querydsl.core.types.dsl.EntityPathBase" in text:
            continue
        if NEEDLE not in text:
            continue
        path.write_text(text.replace(NEEDLE, NEEDLE + EXTRA, 1), encoding="utf-8")


if __name__ == "__main__":
    main()
