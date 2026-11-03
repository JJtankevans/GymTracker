#!/usr/bin/env python3
"""Gera web/catalog.json a partir do catálogo de exercícios do app (ExerciseCatalog.kt).

Uso: python3 tools/gerar_catalogo_web.py
Rode de novo sempre que o catálogo do app mudar (o app web lê só o JSON).
"""
import json, re, sys, pathlib

raiz = pathlib.Path(__file__).resolve().parent.parent
src = (raiz / "shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/catalog/ExerciseCatalog.kt").read_text(encoding="utf-8")

def enum_labels(nome):
    m = re.search(r"enum class %s\(val label: String\) \{(.*?)\n\}" % nome, src, re.S)
    return {k: v for k, v in re.findall(r'(\w+)\("([^"]+)"\)', m.group(1))}

muscles = enum_labels("CatalogMuscle")
equip = enum_labels("Equipment")

items = []
for linha in src.splitlines():
    m = re.match(r'\s*e\("([^"]+)",\s*M\.(\w+),\s*Q\.(\w+),\s*(\d+),\s*(\d+)(.*)\),?\s*$', linha)
    if not m:
        continue
    nome, musc, eq, sets, reps, resto = m.groups()
    hint = re.search(r'hint\s*=\s*"([^"]*)"', resto)
    aliases = re.search(r'aliases\s*=\s*listOf\(([^)]*)\)', resto)
    items.append({
        "name": nome, "muscle": musc, "equipment": eq, "sets": int(sets), "reps": int(reps),
        "hint": hint.group(1) if hint else None,
        "aliases": re.findall(r'"([^"]+)"', aliases.group(1)) if aliases else [],
    })

if len(items) < 80:
    sys.exit("catálogo com poucos itens (%d): o formato de ExerciseCatalog.kt mudou?" % len(items))

saida = raiz / "web/catalog.json"
saida.write_text(json.dumps({"muscles": muscles, "equipment": equip, "exercises": items}, ensure_ascii=False, indent=1), encoding="utf-8")
print("%d exercícios, %d músculos -> %s" % (len(items), len(muscles), saida))
