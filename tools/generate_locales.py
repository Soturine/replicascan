"""One-shot localization generator for Scanora resource catalogs.

The Portuguese product copy is the source for feature modules. The app and
core-ui catalogs already use English as the unqualified fallback.
"""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import argparse
import json
import re
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MODULES = ["app", "core-ui", "feature-camera", "feature-editor", "feature-export", "feature-history", "feature-home", "feature-ocr", "feature-settings"]
TARGETS = {"en": "values", "es": "values-es", "fr": "values-fr", "it": "values-it", "ar": "values-ar"}
TOKEN = re.compile(r"%(?:\d+\$)?[dsf]")

def translate(text: str, target: str) -> str:
    if not text or target == "pt": return text
    protected = {}
    def hold(match):
        key = f"ZXPH{len(protected)}XZ"
        protected[key] = match.group(0)
        return key
    query = TOKEN.sub(hold, text)
    if query in {"Scanora", "PDF", "JPG", "PNG", "OCR", "English", "Español", "Français", "Italiano"}: return query
    url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=pt&tl=" + target + "&dt=t&q=" + urllib.parse.quote(query)
    with urllib.request.urlopen(url, timeout=20) as response:
        data = json.loads(response.read().decode("utf-8"))
    result = "".join(part[0] for part in data[0] if part[0])
    for key, value in protected.items(): result = result.replace(key, value)
    return result.replace("Scanora's", "Scanora")

def all_text_nodes(root):
    nodes = []
    for child in root:
        if child.tag == "string": nodes.append(child)
        elif child.tag == "plurals": nodes.extend(list(child))
    return nodes

def write(tree, path):
    for node in tree.getroot().iter():
        if node.text:
            node.text = node.text.replace("\\'", "'").replace("'", "\\'")
    path.parent.mkdir(parents=True, exist_ok=True)
    ET.indent(tree, space="    ")
    tree.write(path, encoding="utf-8", xml_declaration=True)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--language", choices=TARGETS, help="Generate only one target locale.")
    args = parser.parse_args()
    targets = {args.language: TARGETS[args.language]} if args.language else TARGETS
    for module in MODULES:
        base = ROOT / module / "src/main/res/values/strings.xml"
        if not base.exists(): continue
        pt = ROOT / module / "src/main/res/values-pt-rBR/strings.xml"
        source = pt if pt.exists() else base
        source_tree = ET.parse(source)
        source_nodes = all_text_nodes(source_tree.getroot())
        for language, folder in targets.items():
            tree = ET.parse(source)
            nodes = all_text_nodes(tree.getroot())
            with ThreadPoolExecutor(max_workers=10) as pool:
                translated = list(pool.map(lambda n: translate(n.text or "", language), nodes))
            for node, value in zip(nodes, translated): node.text = value
            write(tree, ROOT / module / f"src/main/res/{folder}/strings.xml")

if __name__ == "__main__": main()
