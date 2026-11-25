#!/usr/bin/env python3
"""Fails when a string or plural in values/ is missing from a translated folder (or the placeholders differ)."""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"
LOCALES = ["de", "it"]
# CLDR plural categories Android asks for (lint MissingQuantity); "many" in it/fr/es/pt is for round millions.
PLURALS = {"de": {"one", "other"}, "it": {"one", "many", "other"}, "fr": {"one", "many", "other"},
           "es": {"one", "many", "other"}, "pt": {"one", "many", "other"}, "pl": {"one", "few", "many", "other"},
           "tr": {"one", "other"}}
PLACEHOLDER = re.compile(r"%(\d+\$)?[sdf]")


def load(folder: Path, quantities: dict | None = None) -> dict:
    out = {}
    for f in sorted(folder.glob("strings*.xml")):
        for el in ET.parse(f).getroot():
            if el.get("translatable") == "false":
                continue
            if el.tag == "string":
                out[el.get("name")] = sorted(PLACEHOLDER.findall("".join(el.itertext())))
            elif el.tag == "plurals":
                items = {i.get("quantity"): "".join(i.itertext()) for i in el}
                out[el.get("name")] = sorted(PLACEHOLDER.findall(items.get("other", "")))
                if quantities is not None:
                    quantities[el.get("name")] = set(items)
    return out


base = load(RES / "values")
errors = []
for loc in LOCALES:
    qty: dict = {}
    tr = load(RES / f"values-{loc}", qty)
    for name, have in qty.items():
        missing = PLURALS.get(loc, {"other"}) - have
        if missing:
            errors.append(f"values-{loc}: plural '{name}' lacks {', '.join(sorted(missing))}")
    for key, ph in base.items():
        if key not in tr:
            errors.append(f"values-{loc}: missing '{key}'")
        elif tr[key] != ph:
            errors.append(f"values-{loc}: placeholders differ in '{key}'")
    for key in tr.keys() - base.keys():
        errors.append(f"values-{loc}: '{key}' does not exist in values/")

print("\n".join(errors) if errors else f"{len(base)} strings translated into {', '.join(LOCALES)}")
sys.exit(1 if errors else 0)
