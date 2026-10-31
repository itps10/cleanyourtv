#!/usr/bin/env python3
"""Aurora Cinema Signature scenic wallpaper, generated reproducibly in GitHub Actions.

Pure landscape: no clock, launcher icons or posters embedded in the JPG.
Uses a single static JPEG on the TV, 1600x900, suitable for memory-limited Android TVs.
This is an original procedural alternative to the previously supplied artwork.
"""
import math
import random
from PIL import Image, ImageDraw, ImageFilter

random.seed(20261031)
W, H = 1600, 900
im = Image.new("RGB", (W, H))
px = im.load()

# Burgundy-to-indigo sky and pink alpine horizon; darker lower water.
for y in range(H):
    if y < 470:
        t = y / 470
        stops = [(0.0, (5, 9, 38)), (0.36, (21, 13, 66)),
                 (0.72, (64, 21, 105)), (1.0, (132, 47, 112))]
    else:
        t = (y - 470) / 430
        stops = [(0.0, (24, 15, 57)), (0.5, (14, 11, 37)),
                 (1.0, (3, 6, 18))]
    for j in range(len(stops) - 1):
        if t <= stops[j + 1][0]:
            a, c1 = stops[j]
            b, c2 = stops[j + 1]
            tt = max(0, min(1, (t - a) / (b - a)))
            base = [c1[k] * (1 - tt) + c2[k] * tt for k in range(3)]
            break
    for x in range(W):
        light = (1.0 + 0.19 * math.exp(-((x - 1050) / 390) ** 2)
                 - 0.27 * abs(x - 800) / 800)
        px[x, y] = tuple(max(0, min(255, int(z * light))) for z in base)

draw = ImageDraw.Draw(im, "RGBA")
# Galaxy stars, clustered above the right side and around the central mountain.
for i in range(1100):
    x = random.randrange(W)
    y = random.randrange(350)
    radius = random.choice([0, 0, 0, 1, 1, 2])
    alpha = random.randint(50, 175)
    hue = (206, 193, 254, alpha) if x > 850 else (209, 214, 246, alpha)
    draw.ellipse((x-radius, y-radius, x+radius+1, y+radius+1), fill=hue)

# An original, faint Milky Way diagonal. Soft overlay, baked once into JPG.
gal = Image.new("RGBA", (W, H), (0, 0, 0, 0))
g = ImageDraw.Draw(gal, "RGBA")
for i in range(220):
    xx = 1000 + i * 2.0 + random.gauss(0, 80)
    yy = 10 + i * 0.9 + random.gauss(0, 60)
    rr = random.randint(3, 18)
    g.ellipse((xx-rr, yy-rr, xx+rr, yy+rr), fill=(148, 73, 230, 35))
gal = gal.filter(ImageFilter.GaussianBlur(16))
im = Image.alpha_composite(im.convert("RGBA"), gal)
draw = ImageDraw.Draw(im, "RGBA")

def ridge(points, color, snow=False):
    draw.polygon([(0, H), *points, (W, H)], fill=color)
    if snow:
        for i in range(1, len(points)-1):
            x, y = points[i]
            if y < 310:
                draw.polygon([(x-31, y+48), (x,y), (x+24,y+55),
                              (x+1,y+31)], fill=(202,189,242,190))

# Detailed distant mountains, with a prominent frosty peak near centre.
ridge([(0,398),(90,344),(190,376),(310,282),(410,361),(520,280),
       (612,322),(774,148),(855,287),(952,233),(1025,338),
       (1143,298),(1275,357),(1412,324),(1600,386)],
      (22,22,66,255), True)
ridge([(0,438),(140,360),(236,408),(340,321),(461,395),
       (568,319),(676,381),(788,248),(892,382),(1001,322),
       (1136,399),(1288,346),(1470,405),(1600,360)],
      (15,18,48,255), True)
ridge([(0,464),(200,428),(380,447),(550,403),(710,438),
       (890,412),(1075,445),(1300,419),(1600,451)],
      (9,14,34,255))

# Dark pine ridges on both sides of the lake.
for side in (0,1):
    for k in range(105):
        x = random.randint(0,520) if side==0 else random.randint(1200,1600)
        base_y = random.randint(404,510)
        height = random.randint(38,140)
        ww = height * random.uniform(0.23,0.42)
        cc = (5,9,18,random.randint(180,240))
        draw.polygon([(x,base_y-height),(x-ww,base_y),(x+ww,base_y)], fill=cc)
        draw.rectangle((x-2,base_y-6,x+2,base_y+10), fill=cc)

# Lake with restrained mirror light and narrow ripples.
draw.line([(0,480),(W,480)],fill=(157,70,148,105),width=3)
for k in range(220):
    y = random.randint(487,850)
    x = random.randint(230,1350)
    length = random.randint(3,95) * (0.5+0.5*(y-480)/420)
    fade = int(90*(1-(y-480)/420))
    draw.line((x-length,y,x+length,y), fill=(161,75,191,max(0,fade)), width=1)
for k in range(135):
    y = random.randint(495,815)
    x = int(1240 + random.gauss(0,75)*(1+(y-480)/500))
    length = random.randint(4,35)
    draw.line((x-length,y,x+length,y),fill=(255,144,80,int(120*(1-(y-480)/420))),width=2)

# A warm-lit modern hillside cabin at far right, away from the launcher labels.
draw.polygon([(1290,405),(1480,334),(1600,365),(1600,500),(1300,500)],
             fill=(10,11,24,255))
draw.polygon([(1260,412),(1480,332),(1600,370),(1600,392),(1481,354),(1280,432)],
             fill=(27,17,33,255))
for ix in range(1300,1600,40):
    for iy in (397,436,468):
        if random.random()<0.85:
            draw.rectangle((ix,iy,ix+24,iy+17), fill=(237,137,54,220))
            draw.line((ix+12,iy,ix+12,iy+17),fill=(35,21,33,255),width=2)

# Dark shoreline and a lower vignette, keeping card text legible.
draw.polygon([(0,824),(290,777),(500,828),(740,794),(990,831),
              (1280,795),(1600,820),(1600,H),(0,H)], fill=(3,6,16,225))
im = im.convert("RGB")
out="aurora_montanhas_lago_sem_interface.jpg"
im.save(out, "JPEG", quality=85, optimize=True, subsampling=2)
print(out, im.size)
