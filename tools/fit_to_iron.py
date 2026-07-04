#!/usr/bin/env python3
"""素材の武器テクスチャから色を取り、iron(基準)の拵えテクスチャの「形・模様・陰影」を
その素材色に塗り替えて、部位テクスチャを生成する。

なぜこの方式か:
  武器テクスチャ全体を そのまま UV領域に貼ると 刃の色が混ざって破綻する。
  そこで「iron_<part> の陰影/模様はそのまま活かし、 色相だけ素材色に置換」する。
  → 金の柄・木の柄 のように、 素材ごとに 綺麗で統一感のある拵えができる。

手順:
  1) 素材元画像から 代表色(不透明・非黒白 の平均) を求める
  2) 基準テクスチャ(iron_<part>) の各画素の明るさを保ったまま、 その代表色で塗る
  3) iron が透明な所は透明のまま ( = iron と同じUV形状 )

使い方:
  python3 tools/fit_to_iron.py <素材元画像> <部位> <出力名> [--weapon katana|tyokuto|rapier]
    部位: 柄=tuka(grip) / 鍔=tuba(guard) / 頭=kasira(pommel)
    --color RRGGBB を付けると 代表色の代わりに その色で塗る
    --mode sample にすると 旧方式(画像をそのまま形にはめる)
例:
  python3 tools/fit_to_iron.py .../block/katanawood3d.png tuka wooden_tuka
  python3 tools/fit_to_iron.py .../item/gold_rapier.png grip gold_grip
  python3 tools/fit_to_iron.py x.png tuka red_tuka --color ff2020
"""
import sys, os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/the_four_primitives_and_weapons/textures")

# 部位 -> (fittingフォルダ, サブフォルダ, 基準テクスチャ名)
PARTS = {
    "katana": {
        "tuka":   ("katana_fitting", "tsuka",  "iron_tuka"),
        "tuba":   ("katana_fitting", "tsuba",  "iron_tuba"),
        "kasira": ("katana_fitting", "kasira", "iron_kasira"),
    },
    "tyokuto": {
        "tuka":   ("straight_katana_fitting", "tsuka",  "tuka"),
        "tuba":   ("straight_katana_fitting", "tsuba",  "tuba"),
        "kasira": ("straight_katana_fitting", "kasira", "kasira"),
    },
    "rapier": {
        "grip":   ("rapier_fitting", "grip",   "grip"),
        "guard":  ("rapier_fitting", "guard",  "guard"),
        "pommel": ("rapier_fitting", "pommel", "pommel"),
    },
}
ALIAS = {"grip": ("rapier", "grip"), "guard": ("rapier", "guard"), "pommel": ("rapier", "pommel")}


def dominant(path):
    """素材元の代表色: 不透明で 黒すぎ/白すぎない画素の平均。"""
    im = Image.open(path).convert("RGBA"); px = im.load()
    rs = gs = bs = n = 0
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            r, g, b, a = px[x, y]
            if a < 10:
                continue
            lum = 0.3 * r + 0.59 * g + 0.11 * b
            if lum < 25 or lum > 245:
                continue
            rs += r; gs += g; bs += b; n += 1
    if n == 0:
        return (150, 150, 150)
    return (rs // n, gs // n, bs // n)


def recolor(refpath, matcol):
    """基準テクスチャの明暗を保ったまま matcol で塗る ( iron形状のまま素材色化 )。"""
    ref = Image.open(refpath).convert("RGBA"); rp = ref.load()
    W, H = ref.size
    tot = cnt = 0
    for y in range(H):
        for x in range(W):
            r, g, b, a = rp[x, y]
            if a < 10:
                continue
            tot += 0.3 * r + 0.59 * g + 0.11 * b; cnt += 1
    ml = max(1.0, tot / max(1, cnt))          # 基準の平均輝度
    mr, mg, mb = matcol
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0)); op = out.load()
    for y in range(H):
        for x in range(W):
            r, g, b, a = rp[x, y]
            if a < 10:
                continue
            lum = 0.3 * r + 0.59 * g + 0.11 * b
            k = lum / ml                       # 平均=素材色そのまま、明暗はそのまま反映
            op[x, y] = (min(255, int(mr * k)), min(255, int(mg * k)), min(255, int(mb * k)), 255)
    return out, (W, H, cnt)


def sample(refpath, srcpath):
    """旧方式: 素材画像をそのまま基準サイズにリサイズし iron の使用領域だけ残す。"""
    ref = Image.open(refpath).convert("RGBA"); rp = ref.load()
    W, H = ref.size
    src = Image.open(srcpath).convert("RGBA").resize((W, H), Image.NEAREST); sp = src.load()
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0)); op = out.load()
    kept = 0
    for y in range(H):
        for x in range(W):
            if rp[x, y][3] >= 10:
                r, g, b, _ = sp[x, y]
                op[x, y] = (r, g, b, 255); kept += 1
    return out, (W, H, kept)


def main():
    argv = sys.argv[1:]
    opts = {}
    pos = []
    i = 0
    while i < len(argv):
        if argv[i] == "--weapon":
            opts["weapon"] = argv[i + 1]; i += 2
        elif argv[i] == "--color":
            opts["color"] = argv[i + 1]; i += 2
        elif argv[i] == "--mode":
            opts["mode"] = argv[i + 1]; i += 2
        else:
            pos.append(argv[i]); i += 1
    if len(pos) < 3:
        print(__doc__); sys.exit(1)
    inp, part, outname = pos[0], pos[1], pos[2]
    weapon = opts.get("weapon", "katana")
    if part in ALIAS and "weapon" not in opts:
        weapon, part = ALIAS[part]
    table = PARTS.get(weapon, {})
    if part not in table:
        print("部位が不正:", part, "/ 使えるのは", list(table.keys())); sys.exit(1)
    fit, sub, ref = table[part]
    refpath = os.path.join(TEX, fit, sub, ref + ".png")
    if not os.path.exists(refpath):
        print("基準テクスチャが無い:", refpath); sys.exit(1)

    mode = opts.get("mode", "recolor")
    if mode == "sample":
        inp = os.path.expanduser(inp)
        if not os.path.exists(inp):
            print("素材元画像が見つかりません:", inp); sys.exit(1)
        out, (W, H, k) = sample(refpath, inp)
        info = "sample %dx%d 使用%dpx" % (W, H, k)
    else:
        if "color" in opts:
            c = opts["color"].lstrip("#")
            col = (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16))
        else:
            inp = os.path.expanduser(inp)
            if not os.path.exists(inp):
                print("素材元画像が見つかりません:", inp); sys.exit(1)
            col = dominant(inp)
        out, (W, H, k) = recolor(refpath, col)
        info = "recolor 素材色=%s (基準%s %dx%d)" % (col, ref, W, H)

    outpath = os.path.join(TEX, fit, sub, outname + ".png")
    out.save(outpath)
    print("生成:", outpath.split("/assets/")[1])
    print(" ", info)


if __name__ == "__main__":
    main()
