#!/usr/bin/env python3
"""Corta uma folha de ilustrações de exercícios (grade de desenhos sobre fundo escuro) em PNGs 384x384 transparentes,
no formato usado pelo app (docs/IMAGENS_EXERCICIOS.md).

Uso:
  python3 tools/cortar_folha.py docs/folhas/folha1.jpeg --out /tmp/cortes                 # só corta: cell_L1_C1.png ...
  python3 tools/cortar_folha.py nova.png --keys chaves.txt --out app/src/main/res/drawable-nodpi
  python3 tools/cortar_folha.py --faltando                                                # lista as imagens que faltam no catálogo

--keys: arquivo com uma chave por linha (ex.: ex_hack_squat), na ordem de leitura da folha (esquerda->direita, de cima
para baixo). Use "-" para pular uma célula. Sem --keys o script só numera as células, para você conferir antes de nomear.

Como funciona: fundo = cor mais comum das bordas; "figura" = pixel que difere do fundo; faixas de linhas e colunas vazias
separam os desenhos; o fundo conectado à borda de cada desenho vira transparente (com borda suave) e os "bolsões"
fechados dentro da figura ficam opacos; o desenho é centralizado num quadrado de 384x384 com ~8% de margem.
Só precisa de Pillow e numpy.
"""
import argparse, os, re, sys, unicodedata
import numpy as np
from PIL import Image, ImageFilter

SIZE = 384
MARGIN = 0.08
THR_BG = 34        # distância de cor (soma RGB) abaixo da qual é fundo
THR_SOFT = 70      # entre THR_BG e THR_SOFT a borda fica semitransparente
MIN_GAP = 6        # linhas/colunas vazias seguidas que separam desenhos
MIN_PIECE = 12     # ignora faixas menores que isso (poeira)

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(REPO, "shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/catalog/ExerciseCatalog.kt")
DRAWABLES = os.path.join(REPO, "app/src/main/res/drawable-nodpi")


def key_for(name):
    n = unicodedata.normalize("NFD", name).encode("ascii", "ignore").decode().lower()
    return "ex_" + re.sub(r"_+", "_", re.sub(r"[^a-z0-9]+", "_", n)).strip("_")


def catalog_names():
    src = open(CATALOG, encoding="utf-8").read()
    return re.findall(r'^\s*e\("([^"]+)"', src, re.M)


def faltando():
    have = {f[:-4] for f in os.listdir(DRAWABLES) if f.startswith("ex_") and f.endswith(".png")}
    miss = [(n, key_for(n)) for n in catalog_names() if key_for(n) not in have]
    print(f"{len(miss)} sem imagem de {len(catalog_names())}:")
    for n, k in miss:
        print(f"  {k}.png   <- {n}")


def background(arr):
    edge = np.concatenate([arr[0], arr[-1], arr[:, 0], arr[:, -1]]).reshape(-1, 3)
    vals, counts = np.unique((edge // 8) * 8, axis=0, return_counts=True)
    return vals[counts.argmax()].astype(int) + 4


def runs(profile, min_gap):
    """Intervalos [a, b) onde profile > 0, juntando vãos menores que min_gap."""
    idx = np.flatnonzero(profile > 0)
    if idx.size == 0:
        return []
    out, start, prev = [], idx[0], idx[0]
    for i in idx[1:]:
        if i - prev > min_gap:
            out.append((start, prev + 1)); start = i
        prev = i
    out.append((start, prev + 1))
    return [(a, b) for a, b in out if b - a >= MIN_PIECE]


def cells(mask):
    """Faixas horizontais (linhas da grade) e, dentro de cada uma, as colunas -> caixas (x0, y0, x1, y1)."""
    boxes = []
    for y0, y1 in runs(mask.sum(axis=1), MIN_GAP):
        for x0, x1 in runs(mask[y0:y1].sum(axis=0), MIN_GAP):
            sub = mask[y0:y1, x0:x1]
            ys = np.flatnonzero(sub.sum(axis=1) > 0)
            boxes.append((x0, y0 + ys[0], x1, y0 + ys[-1] + 1))
    return boxes


def make_transparent(crop_rgb, bg):
    d = np.abs(crop_rgb.astype(int) - bg).sum(axis=2)
    is_bg = d < THR_BG
    # fundo = parte "parecida com o fundo" ligada à borda (espalha a partir da borda)
    reach = np.zeros_like(is_bg)
    reach[0, :] = is_bg[0, :]; reach[-1, :] = is_bg[-1, :]; reach[:, 0] = is_bg[:, 0]; reach[:, -1] = is_bg[:, -1]
    while True:
        grown = reach.copy()
        grown[1:, :] |= reach[:-1, :]; grown[:-1, :] |= reach[1:, :]
        grown[:, 1:] |= reach[:, :-1]; grown[:, :-1] |= reach[:, 1:]
        grown &= is_bg
        if (grown == reach).all():
            break
        reach = grown
    alpha = np.where(reach, 0, 255).astype(np.uint8)
    # borda suave: pixels logo ao lado do fundo e pouco diferentes dele ficam semitransparentes
    near = np.asarray(Image.fromarray(reach.astype(np.uint8) * 255).filter(ImageFilter.MaxFilter(3))) > 0
    soft = near & ~reach & (d < THR_SOFT)
    alpha[soft] = (255 * (d[soft] - THR_BG) / (THR_SOFT - THR_BG)).clip(0, 255).astype(np.uint8)
    return np.dstack([crop_rgb, alpha])


def fit(rgba):
    img = Image.fromarray(rgba, "RGBA")
    inner = int(SIZE * (1 - 2 * MARGIN))
    scale = min(inner / img.width, inner / img.height)
    img = img.resize((max(1, round(img.width * scale)), max(1, round(img.height * scale))), Image.LANCZOS)
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    out.paste(img, ((SIZE - img.width) // 2, (SIZE - img.height) // 2), img)
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folha", nargs="?")
    ap.add_argument("--out", default="cortes")
    ap.add_argument("--keys", help="arquivo com uma chave por linha, na ordem de leitura; '-' pula a célula")
    ap.add_argument("--faltando", action="store_true", help="lista as imagens do catálogo que ainda não existem")
    a = ap.parse_args()
    if a.faltando:
        return faltando()
    if not a.folha:
        ap.error("informe a folha ou use --faltando")

    rgb = np.asarray(Image.open(a.folha).convert("RGB"))
    bg = background(rgb)
    mask = np.abs(rgb.astype(int) - bg).sum(axis=2) >= THR_BG
    boxes = cells(mask)
    keys = [l.strip() for l in open(a.keys, encoding="utf-8")] if a.keys else None
    if keys is not None and len(keys) != len(boxes):
        sys.exit(f"A folha tem {len(boxes)} desenhos mas --keys tem {len(keys)} linhas. Confira (use '-' para células vazias/puladas).")

    os.makedirs(a.out, exist_ok=True)
    pad = 3
    for i, (x0, y0, x1, y1) in enumerate(boxes):
        x0, y0 = max(0, x0 - pad), max(0, y0 - pad); x1, y1 = min(rgb.shape[1], x1 + pad), min(rgb.shape[0], y1 + pad)
        name = keys[i] if keys else f"cell_{i + 1:02d}"
        if name == "-":
            continue
        fit(make_transparent(rgb[y0:y1, x0:x1], bg)).save(os.path.join(a.out, name.removesuffix(".png") + ".png"), optimize=True)
    print(f"{len(boxes)} desenhos encontrados -> {a.out}")


if __name__ == "__main__":
    main()
