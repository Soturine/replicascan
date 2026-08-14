"""Fail when a supported Scanora locale is incomplete or breaks placeholders."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MODULES = ["app", "core-ui", "feature-camera", "feature-editor", "feature-export", "feature-history", "feature-home", "feature-ocr", "feature-settings"]
LOCALES = [
    "values", "values-pt-rBR", "values-es", "values-fr", "values-it",
    "values-ar", "values-de", "values-id", "values-hi", "values-tr",
    "values-ja", "values-ko",
]
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[dsf]")

def catalog(path):
    result = {}
    root = ET.parse(path).getroot()
    for child in root:
        if child.tag == "string" and child.attrib.get("translatable", "true") != "false": result[child.attrib["name"]] = (child.text or "",)
        elif child.tag == "plurals":
            result[child.attrib["name"]] = tuple(
                (item.attrib.get("quantity", ""), item.text or "") for item in child
            )
    return result

errors = []
for module in MODULES:
    paths = {locale: ROOT / module / f"src/main/res/{locale}/strings.xml" for locale in LOCALES}
    missing_files = [str(path.relative_to(ROOT)) for path in paths.values() if not path.exists()]
    if missing_files:
        errors.append(f"{module}: missing catalogs: {', '.join(missing_files)}")
        continue
    catalogs = {locale: catalog(path) for locale, path in paths.items()}
    base_keys = set(catalogs["values"])
    for locale, entries in catalogs.items():
        if set(entries) != base_keys:
            errors.append(f"{module}/{locale}: key mismatch missing={sorted(base_keys-set(entries))} extra={sorted(set(entries)-base_keys)}")
            continue
        for key, values in entries.items():
            base_values = catalogs["values"][key]
            is_plural = bool(base_values and isinstance(base_values[0], tuple))
            if is_plural:
                base_quantities = [quantity for quantity, _ in base_values]
                locale_quantities = [quantity for quantity, _ in values]
                if len(locale_quantities) != len(set(locale_quantities)):
                    errors.append(f"{module}/{locale}/{key}: duplicate plural quantities {locale_quantities}")
                missing_quantities = sorted(set(base_quantities) - set(locale_quantities))
                if missing_quantities:
                    errors.append(f"{module}/{locale}/{key}: missing plural quantities {missing_quantities}")
                if "other" not in locale_quantities:
                    errors.append(f"{module}/{locale}/{key}: plural must define other")
                base_texts = [value for _, value in base_values]
                locale_texts = [value for _, value in values]
            else:
                base_texts = list(base_values)
                locale_texts = list(values)
            base_tokens = sorted(set(token for value in base_texts for token in PLACEHOLDER.findall(value)))
            for value in locale_texts:
                locale_tokens = sorted(set(PLACEHOLDER.findall(value)))
                if base_tokens != locale_tokens:
                    errors.append(f"{module}/{locale}/{key}: placeholder mismatch {base_tokens} != {locale_tokens}")
            if any(not value.strip() for value in locale_texts):
                errors.append(f"{module}/{locale}/{key}: blank translation")
            if locale in {"values-fr", "values-it"} and any("'" in value.replace("\\'", "") for value in locale_texts):
                errors.append(f"{module}/{locale}/{key}: apostrophe must be escaped for AAPT")

if errors:
    print("\n".join(errors))
    sys.exit(1)
print("Localization catalogs complete: 9 modules x 12 locales")
