"""Convert quantity copy to Android plurals for all supported locales."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DATA = {
    "feature-editor": {"editor_review_summary": {
        "values": ("%1$d page in the batch.", "%1$d pages in the batch."), "values-pt-rBR": ("%1$d página no lote.", "%1$d páginas no lote."),
        "values-es": ("%1$d página en el lote.", "%1$d páginas en el lote."), "values-fr": ("%1$d page dans le lot.", "%1$d pages dans le lot."), "values-it": ("%1$d pagina nel lotto.", "%1$d pagine nel lotto.")}},
    "feature-export": {
        "export_summary": {"values": ("%1$d page ready to export.", "%1$d pages ready to export."), "values-pt-rBR": ("%1$d página pronta para exportação.", "%1$d páginas prontas para exportação."), "values-es": ("%1$d página lista para exportar.", "%1$d páginas listas para exportar."), "values-fr": ("%1$d page prête à exporter.", "%1$d pages prêtes à exporter."), "values-it": ("%1$d pagina pronta per l'esportazione.", "%1$d pagine pronte per l'esportazione.")},
        "export_success_message": {"values": ("%1$d file ready to open or share.", "%1$d files ready to open or share."), "values-pt-rBR": ("%1$d arquivo pronto para abrir ou compartilhar.", "%1$d arquivos prontos para abrir ou compartilhar."), "values-es": ("%1$d archivo listo para abrir o compartir.", "%1$d archivos listos para abrir o compartir."), "values-fr": ("%1$d fichier prêt à ouvrir ou partager.", "%1$d fichiers prêts à ouvrir ou partager."), "values-it": ("%1$d file pronto per essere aperto o condiviso.", "%1$d file pronti per essere aperti o condivisi.")}},
    "feature-ocr": {"ocr_supporting": {"values": ("%1$d organized section.", "%1$d organized sections."), "values-pt-rBR": ("%1$d trecho organizado.", "%1$d trechos organizados."), "values-es": ("%1$d fragmento organizado.", "%1$d fragmentos organizados."), "values-fr": ("%1$d extrait organisé.", "%1$d extraits organisés."), "values-it": ("%1$d sezione organizzata.", "%1$d sezioni organizzate.")}},
}

for module, keys in DATA.items():
    for folder in ("values", "values-pt-rBR", "values-es", "values-fr", "values-it"):
        path = ROOT/module/"src/main/res"/folder/"strings.xml"
        tree = ET.parse(path); root = tree.getroot()
        for key, locales in keys.items():
            old = root.find(f"string[@name='{key}']")
            if old is not None:
                index = list(root).index(old); root.remove(old)
                plural = ET.Element("plurals", {"name": key})
                one, other = locales[folder]
                for quantity, text in (("one", one), ("other", other)):
                    item = ET.SubElement(plural, "item", {"quantity": quantity}); item.text = text
                root.insert(index, plural)
        ET.indent(tree, space="    "); tree.write(path, encoding="utf-8", xml_declaration=True)
