# Railshot art playbook

Steal the **machine language** of 90s arcade volley games, not anyone else’s roster. All art is original Railshot IP.

## Style lock (every prompt)

Paste this on every generation:

```
1994 Neo Geo / CPS1 arcade pixel art, 4:3, high-resolution pixel art for a modern phone, 1440x1080 canvas, chunky pixels still visible, limited 32-color palette, hard 1px outlines, dithering, no anti-aliasing, no photorealism, no 3D, no modern UI, original IP for a game called Railshot, 1990s arcade fighting-game anatomy, adult characters only, do not copy Battle Flip Shot, Street Fighter, or King of Fighters characters, no logos from other games
```

If a model drifts: `same character as previous image, identical face, hair, and colors`.

Do not paste Flip Shot screenshots into the generator as style refs.

## Technical specs

Canonical arena: **1440×1080** (4:3). That matches the short side of most 1080p phones in landscape, so the court is pixel-even on height and letterboxed left/right on 16:9 / 20:9. QHD and iPhone scale up slightly; we do not author a second atlas.

Keep the 90s pixel look; just draw it on a phone-sized canvas. Do not generate 384×288 and blow it up.

| Use | Size | Background |
| --- | --- | --- |
| Courts, full screens | **1440×1080** | Opaque scene |
| Cabinet frame overlay | **1440×1080** (4:3) | Magenta `#FF00FF` in every inset (HUD wells + 4:3 playfield hole) |
| In-game fighters | **192×233** | Magenta `#FF00FF` — final packed size for every idle/walk/hit frame |
| Face tiles | **192×192** | Magenta `#FF00FF`. **No drawn border.** Content flush left, right, and bottom. |
| Standing select portraits | **480×720** | Magenta `#FF00FF` |
| VS busts | **480×600** | Magenta `#FF00FF` |
| Word banners | **960** wide (`FIGHT`/`ROUND`/`YOU WIN`/`YOU LOSE` height fits letters; `VS` **384×384**) | Magenta `#FF00FF` |
| Digits | **120×150** each | Magenta `#FF00FF` |
| Ball | **60×60** | Magenta `#FF00FF` |
| Ice ball | **60×60** | Magenta `#FF00FF` |
| Ice burst | **128×128** | Magenta `#FF00FF` |
| Court wall | **96×176** | Magenta `#FF00FF` |
| Court trace | **48×192** | Magenta `#FF00FF` |
| Court hawk | **136×192** | Magenta `#FF00FF` |
| Court X | **192×192** | Magenta `#FF00FF` |
| Court log | **240×40** | Magenta `#FF00FF` |
| Life chips | standing **22×120**, down **48×120** | Magenta `#FF00FF` |
| Nameplates | **480×90** | Magenta `#FF00FF` |
| Ending still | **1440×840** letterboxed in 4:3 | Opaque |

## Chroma lock (every keyed sprite)

**One key color:** magenta `#FF00FF` (RGB 255, 0, 255). Every sprite that composites over wallpaper, court, or another layer uses this field. Not “close enough” pink.

Applies to faces, select full-bodies, VS busts, court idle/walk/hit, nameplates, `PLAYER SELECT`, `VS`, banners, ball, chips — the whole keyed set.

**After generation, rewrite the backdrop to exact `#FF00FF`.** Flood only the field (including pink trapped in hair gaps). Do not eat the drawing. Do not leave the generator’s hot pink, salmon, or black.

**No bleed.** Magenta is a hard field, not a wash. It must not mix into outlines, hair, skin, metal, or clothes — no pink halo, no salmon fringe, no dark fuchsia leftovers along the silhouette. After the field rewrite, **defringe**: leftover field-pink pixels become `#FF00FF`; mixed edge pixels are pulled to the nearest sprite color (or dropped to the key if they are still mostly magenta). If a halo is visible after chroma-key, the PNG is wrong. The engine will not hide near-pink.

**Forbidden as a key:** hot pink, fuchsia that is not `#FF00FF`, black, checkerboard, or alpha-only RGB leftovers. Do not invent a second key per character or per screen. 2P violet clothes are **not** the key.

**Engine:** key `#FF00FF` only (a few levels of tolerance for compression). Do not key purple jackets.

## Art on disk

Canonical sprites live in **`art/`** (`art/{name}/…png`, `art/ui_*.png`). Do **not** copy them into `res/drawable` or `drawable-nodpi`. The app reads `art/` as Android assets. Edit and generate only under `art/`.

**Launcher icon** is the exception: source `art/ui_launcher.png` (flaming steel play-ball, arcade teal plate, **no magenta**, not a fighter). Pack into `res/mipmap-*` and adaptive `ic_launcher_foreground`. Do not use a roster portrait.

The Android stage is a centered **4:3 cabinet** (`{name}/{name}_ui_cabinet.png`, 1440×1080), chosen from the **2P / rival** fighter. **Define inset pixels first** (`World` cabinet constants). Draw / generate the cabinet **around those rects** so chrome and wells are the same layout. Never measure wells after the fact and nudge HUD to match a mismatched PNG. The **playfield** is a **4:3 magenta inset** (1152×864). Court floors `FIT_XY` in that inset. HUD text is **typeset in code** and **scaled to the well** so it stays inside. Do not use name sprites in cabinet wells.

## Audio

WAV clips live in `app/src/main/res/raw/` (16-bit PCM, 44.1 kHz, uncompressed). `SoundManager` is SoundPool SFX + dual MediaPlayer BGM, same shape as WW2 Blitz. Original IP only — do not rip Flip Shot or other cabinets.

| Cue | File | When |
| --- | --- | --- |
| Arrow | `sfx_arrow.wav` | Select roster arrows |
| Select | `sfx_select.wav` | SELECT confirm |
| Round 1/2/3 | `sfx_round.wav` then `sfx_1.wav` / `sfx_2.wav` / `sfx_3.wav` | Enter `Phase.ROUND` (announcer) |
| Fight | `sfx_fight.wav` | Enter `Phase.SERVE` |
| You win | `sfx_win.wav` | `ui_you_win.png` on `Phase.SET_WIN` |
| You lose | `sfx_lose.wav` | `ui_you_lose.png` on `Phase.SET_WIN` |
| Shield hit | `sfx_shield.wav` | Ball bounces off a fighter |
| Chip off | `sfx_chip.wav` | Ball kills a life tile |
| Ice shatter | `sfx_ice.wav` | Ice comet smashed or flattening a gate |
| Court bounce | `sfx_wall.wav` | Fireball or ice comet hits a rail, Ash wall, Rivet trace, Hex car, Quill hawk, Kite X, or Maru log |
| Match BGM | `bgm_match.wav` | Loops from app start |

## Produce a sprite (mandatory)

The generator will ignore size, exact magenta, and “mirror.” Do not trust it. After every generate:

1. **Rewrite the field** to exact `#FF00FF` (flood backdrop + field-pink hair gaps only). **Defringe the silhouette** so no magenta bleeds into the drawing.
2. **Pack to the table size** (nearest-neighbor). Never clip. Uniform scale; extra magenta is padding.
3. **Save only under `art/`.**
4. **Mirrors are code, not the model.** `vs_right` and all `*_right` court frames = bitmap horizontal flip + 2P recolor. Do not GenerateImage a mirror.
5. **Stop for approval** on **A**, then **idle_left**, then **vs_left**. Do not generate walk/hit or vs_right until those are approved.
6. **Wire** new files in `Fighter.art()` asset paths. Placeholders stay until the file exists.

**`idle_left` generate inputs (two images, named in the prompt):**

1. **Proportion + cartoon face.** Men: `art/ash/ash_game_idle_left.png`. Women: `art/rivet/rivet_game_idle_left.png`. Copy body math and face language only — big **anime fighter eyes with a colored iris** (like Ash/Rivet idle, not googly white discs), head about a third of the sprite, short thick muscular torso, stubby legs, oversized boots. Do not copy that fighter’s hair, clothes, or weapon. If adding packed A as a second image makes the **file on disk** a scaled-down A, drop A from the generate and describe clothes in text.
2. **Clothes / hair / beard / shield only:** that fighter’s packed A (`art/{name}/{name}_select_fullbody.png`). Do not copy A’s adult anatomy or A’s portrait face (small eyes, realistic scowl).

**Court idle anatomy lock.** Super-deformed paddle sprite. Head is about a third of the sprite. Torso is short and thick **muscle** (a waist, not a round gut). Legs are stubby. Boots are oversized. Same bulky chibi silhouette as the Ash/Rivet idle. Big cartoon eyes. Clothes from A can match and the sprite still fails if the body or face is not this.

**Prompt words.** Use: bulky chibi, thick muscle, short torso, stubby legs, big head, big cartoon eyes. **Do not use:** chubby, fat, obese, beer belly, gut, round stomach.

**Wrong (discard, do not pack):** scaled-down A; small head; long fighter legs; obese belly; tiny realistic eyes. Unique filename per attempt. **Read the PNG on disk** before packing (the generate preview can lie).

A faces **left**. `idle_left` must face **right** (nose right, shield on the right side of the PNG). If it faces left, **horizontal-flip the bitmap**. Do not GenerateImage a second pose. **No drop shadow.** Pack **192×233**, `#FF00FF`. Walk and hit only after idle is approved, from that idle (pose only).

## Roster

Three women, three men. Lock faces and bodies here first. Do not invent extra fighters until each of the six has every sprite in **Character art set**.

### Women (sexy 90s arcade)

Long legs, slim waist, large bust, adult cheesecake proportions like 1994 fighting games. Revealing but still a fighter costume (shield in hand). Distinct hair color is mandatory.

| ID | Name | Hair | Body and costume |
| --- | --- | --- | --- |
| 1 | **Rivet** | **Blonde**, long, wind-swept | Tall, long-legged, large bust. Teal mechanic: visor pushed up, cropped jacket, short shorts, thigh boots, wrench-shaped shield. Confident, sexy idle. |
| 5 | **Quill** | **Red**, long, with a bird-crest | Tall, long-legged, large bust. Feathered cape, cropped top, high-cut bottoms, talon boots, kite shield. Predatory smile. |
| 6 | **Hex** | **Brunette**, long, messy, dark | Tall, long-legged, large bust. Half-Asian punk brunette: no hat, cropped dark-violet leather jacket, choker, short shorts, combat boots, strapped punk buckler. Cool, seductive stare. |

Always include in female prompts: `adult woman, sexy 1990s arcade fighter, long legs, large breasts, slim waist, cheesecake posing, not a child, not realistic`.

### Men (military / Street Fighter body)

Tall, thick muscle, broad shoulders, fighter stance. Military or combat-athlete costume, not slim, not lanky, not a mascot.

| ID | Name | Hair | Body and costume |
| --- | --- | --- | --- |
| 2 | **Ash** | Short black, undercut | Huge street-fighter build, scarred, red combat jacket open on a muscled chest, dog tags, round buckler, cargo pants, boots. |
| 3 | **Kite** | **Blond**, short, high-and-tight | Very tall military striker, V-torso, aviator sunglasses, olive flight suit unzipped at the collar, harness, kite-shaped riot shield, jump boots. Top Gun pilot energy, not Ash. |
| 4 | **Maru** | **Red**, viking braids | Tall heavyweight bruiser, biggest of the three, Scottish highland kit: tartan kilt, sporran, khaki sleeveless shirt, disc riot shield, wrapped fists, highland boots. |

Always include in male prompts: `adult man, tall, extremely muscular, military fighter, Street Fighter 2 body type, broad shoulders, thick arms, not skinny`.

**Facing.** Select full-body **A** and face tile **B** both face **left** (nose / gaze toward the left edge of the PNG). They sit on the right of character select and must look toward the left UI. If the generator draws them facing right, **horizontal-flip the bitmap** — do not GenerateImage a new pose. Left-court and `vs_left` face **right**. Right-court and `vs_right` face **left**. Those court/VS right-facing→left files are flipped in bitmap code, not drawn as a new pose.

## Character art set

Every fighter needs pieces **A–F**. A–E share the same face and costume. The **name sprites** are letters only. Generate in this order per character (Rivet first, then Ash, Kite, Maru, Quill, Hex).

| # | Piece | Size | Use |
| --- | --- | --- | --- |
| A | **Select full-body** | **480×720** | Character select, **right** side of the screen. Standing, full-body, **left-facing** 3/4 (like Rivet). This is the **face lock**. |
| B | **Face tile** | **192×192** | Character select roster strip. Same **left-facing** head as A. |
| C | **VS profile** | **480×600** | `{name}_vs_left.png` = P1. `{name}_vs_right.png` = horizontal flip of vs_left + 2P clothing colors |
| D | **In-game paddle** | **192×233** | Left: `idle_left` / `walk_left` / `hit_left`. Right: same with `_right`, from left + vs_right |
| E | **Ending full-body** | **480×720** (or full 1440×1080 card) | Congratulations / No.1 screen |
| F | **Name sprites** | **480×90** | `{name}_name_select.png` on select. `{name}_name_vs.png` under VS busts |

### Consistency rules (all characters)

1. **Face lock.** Piece **B** (and C, D, E) must match piece **A**: same face, same hair, same visor/hat if A has one. Crop the head from A; do not invent a new face. Hair matches A (Maru: red viking braids). Do not swap a man’s hair for a woman’s cut from another fighter.
2. **VS left is P1.** `{name}_vs_left.png` uses the **same clothing as full-body (A)** and the left court sprites. **Build vs_right only after vs_left exists** — bitmap flip + 2P clothes. Do not generate vs_right.
3. **In-game sprites are animation frames, not portraits.** Idle / walk / hit are bulky, cartoony 1994 court sprites (Battle Flip Shot: big head, thick body, short legs, shield = paddle). They replace the paddle and are **swapped on top of the same world position** to animate move and hit. **Costume must match piece A** (see overlay rules). Not a pinup.
4. **Hit has no ball.** The ball is a separate sprite composited in playback. Hit is only the character pose (shield swing / strike). No ball, no projectile, no extra orb.
5. **Two name sprites, no portraits as input.** `{name}_name_select.png` is a **small blue plate with yellow letters** (select, under PLAYER SELECT on the left). `{name}_name_vs.png` is **blue letters with a white edge**, no plate (VS, under each bust). Do not typeset names in code when the sprite exists. Do not use one file for both screens.

### 2P apparel palettes

Hair and skin never change. Only clothes, visor, metal trim, and shield tint.

| Character | P1 (VS left, first select, left court) | P2 (VS right, mirror match) |
| --- | --- | --- |
| Rivet | Teal jacket, teal boots, gold buckles, teal visor | Magenta-violet jacket and boots, silver buckles, purple visor |
| Ash | Red open jacket, gold trim, brown boots, steel buckler | Blue open jacket, silver trim, dark boots, steel buckler |
| Kite | Olive flight suit, tan harness, jump boots, gold aviators | Desert-tan flight suit, olive harness, same boots, silver aviators |
| Maru | Green/khaki tartan kilt, khaki shirt, leather sporran, wrapped fists | Steel-blue tartan kilt, steel-blue shirt, dark sporran, wrapped fists |
| Quill | Earth/feather costume, warm browns and reds (hair stays red) | Cool purple/blue feather costume (hair stays red) |
| Hex | Dark violet leather, silver studs, strapped punk buckler | Teal-black leather, gold studs, strapped punk buckler |

### Character locks (paste after the style lock)

**Rivet**
```
Rivet, adult blonde woman, long wind-swept blonde hair, visor pushed up, teal cropped mechanic jacket, short shorts, thigh boots, large breasts, very long legs, sexy 1990s arcade fighter, wrench-shaped shield, original Railshot character
```

**Ash**
```
Ash, adult man, short black undercut, extremely muscular Street Fighter body, broad shoulders, red combat jacket open, dog tags, scar, cargo pants, round buckler, military-street hybrid, original Railshot character
```

**Kite**
```
Kite, adult man, high-and-tight blond hair, aviator sunglasses, very tall, thick muscle, V-torso, olive military flight suit, harness, jump boots, kite-shaped riot shield, Top Gun fighter-pilot look, original Railshot character
```

**Maru**
```
Maru, adult man, bright red viking hair with braids, tallest heavyweight, massive chest and arms, Scottish highland fighter, tartan kilt, sporran, khaki sleeveless shirt, wrapped fists, disc riot shield, highland boots, original Railshot character
```

**Quill**
```
Quill, adult red-haired woman, long red hair, bird-crest, feathered cape, cropped top, high-cut bottoms, talon boots, large breasts, very long legs, sexy 1990s arcade fighter, kite shield, original Railshot character
```

**Hex**
```
Hex, adult half-Asian brunette woman, East Asian mixed features, long messy dark espresso-brown hair, no hat, punk, cropped dark-violet leather jacket, black choker, short shorts, combat boots, large breasts, very long legs, sexy 1990s arcade fighter, strapped round punk buckler, original Railshot character
```

### A — Select full-body

**Left pose, shown on the right.** Piece A is drawn on the **right** of character select, next to the left UI stack. Every fighter uses a **left-facing 3/4 standing pose** like Rivet: body and gaze toward the **left** (toward PLAYER SELECT), weight in a heroic idle, full body in frame. Do **not** face A to the right (that is `vs_left` / court). If A faces right, **horizontal-flip the bitmap**. Do not GenerateImage a mirror of A. Pose reference: `art/rivet/rivet_select_fullbody.png` (direction only — copy that facing, not Rivet’s clothes or body).

```
Large 1994 arcade standing portrait, [CHARACTER LOCK], full body, 3/4 view FACING LEFT toward the left edge (same pose direction as Rivet select full-body), heroic or sexy idle, pixel art, no text, 480x720, magenta background #FF00FF
```

### B — Face tile (character select)

Must look like a close crop of **A**: same face, same hair (long hair on the women, male cut on the men), same eyes, same visor/hat if A has one, **same left-facing direction**. Use A as the reference image. If B faces right, horizontal-flip the bitmap. Do not GenerateImage a mirror.

**No outline on the portrait.** Do **not** bake a frame into the face PNG. Roster tiles use a **drawn arcade rectangle** (idle grey, blinking gold when the cursor is on the tile; no inner ring, no corner ticks) Pack **192×192**. After removing any generator frame, the drawing must touch the **left, right, and bottom** edges — no magenta padding on those three sides. Magenta may sit **above** the hair only. Do not clip the face; if the crop is too tall, scale so width fills 192 and sit on the bottom (trim extra from the top field, not the chin).

```
Square 1994 arcade face portrait cropped from the select full-body of [CHARACTER LOCK], identical face and hair, head and shoulders FACING LEFT (nose toward the left edge, same direction as the full-body), visor/hat only if the full-body has one, NO colored border, NO frame, NO outline rectangle, 192x192, magenta background #FF00FF
```

Roster strip (all six once faces exist):

```
Row of six square 192x192 arcade face portraits, NO colored borders on the portraits, 1994 pixel faces of Rivet Ash Kite Maru Quill Hex in that order, one 1P cursor box drawn in UI, magenta around the row
```

### C — VS profile

**`{name}_vs_left.png` first.** Same outfit as A and as the court animation set. This is fighter 1: left VS slot, left court.

**Tight bust, like Rivet.** VS is a **zoomed head-and-shoulders** fill of **480×600**, not a waist-up figure in empty magenta. Match the crop of `art/rivet/rivet_vs_left.png`: huge face, shoulders, upper chest; head near the top; very little field around the silhouette. Do not show belt, hips, or a tiny character. Framing reference is Rivet’s vs_left (crop only — not her face or clothes).

```
Large arcade bust, TIGHT head-and-shoulders close-up filling the frame like Rivet vs_left, [CHARACTER LOCK], P1 apparel matching the full-body sprite, facing toward the right (into the VS center), hair and face matching the select full-body, 480x600, magenta background #FF00FF
```

**`{name}_vs_right.png`** is **vs_left flipped in bitmap code** (not generated), with **2P clothing colors**, for the right VS slot.

Do not GenerateImage a mirror. Recolor apparel to the 2P palette (hair and skin unchanged), then horizontal-flip so they face left into the VS center. Same face. Pack **480×600**, magenta `#FF00FF`.

File names must include `vs_left` / `vs_right` (example: `rivet_vs_left.png`).

### D — In-game paddle sprites

These replace the paddle. The game **does not pan inside the PNG**. It draws whichever frame is active (idle, walk, hit) at the **same court position** and the **same on-screen size** for every fighter (packed **192×233**, baseline paddle height). Kit `paddleLen` is the **hitbox only**. Do not scale the sprite to the hitbox.

#### Overlay animation rules

1. **`idle_left` = Ash/Rivet court body + A clothes.** Two image refs, named in the prompt: **(1)** Ash idle (men) or Rivet idle (women) for proportion and cartoon face (big eyes); **(2)** packed **A** for hair, outfit, beard, and paddle-shaped shield only. Anatomy lock: super-deformed paddle sprite, head about a third of the sprite, short thick muscular torso, stubby legs, oversized boots. **Reject** a miniature of A, an obese belly, or A’s portrait eyes. Face **right**. **No drop shadow.** Pack **192×233**. Walk and hit are **that idle with a new pose**.
2. **Idle first, then pivots.** Generate and approve **idle_left** against A. Only then generate **`walk_left`** and **`hit_left`** using that idle as the reference (`same character, identical clothes, only the pose changes`). Three left-court files: `{name}_game_idle_left.png`, `{name}_game_walk_left.png`, `{name}_game_hit_left.png`. These pair with `{name}_vs_left.png` and play on the **left side of the court**.
3. **No shadows.** No drop shadow, ground blob, contact shadow, or floor ellipse. Magenta `#FF00FF` goes right up to the boots.
4. **Same character height as idle.** After cropping magenta, scale every frame **uniformly** so its bounding-box **height equals idle’s height**. Walk and hit often come out larger — shrink them to idle. Width scales with height (no squash).
5. **Shared box, then pack to 192×233.** After height-match, `W = max(content widths)`, `H = idle height`. Place every frame in a temporary `W×H` box, bottom-aligned, horizontally centered, **never clipped**. Then scale that shared box **uniformly** (nearest-neighbor) to **fit 192×233**. Final file is always **192×233** with `#FF00FF` padding. Do not ship a custom canvas size. Rivet is the packed example: `art/rivet/rivet_game_*_{left,right}.png`.
6. **Placement in the final box.** Feet on the bottom row. Center horizontally. Hair of the tallest pose (idle height-match) sits on the **top** row — do not leave a magenta band above the character. Magenta may sit **beside** slimmer poses (idle vs hit). Clipping is forbidden — if hit is too wide, the shared scale shrinks **all** frames until hit fits.
7. **Shared camera.** Walk is a **stride pose**, not a character sliding across the PNG. Hit is a **swing pose** with feet planted. Onion-skin: heads, hips, and feet overlap; only limbs and shield differ.
8. **Three left-court frames: idle_left, walk_left, hit_left.** While moving, show **`walk_left`**. When still, **`idle_left`**. On contact, **`hit_left`** for a few ticks, then back. **No ball** in hit art. No two-frame walk cycle.
9. **Shield is the paddle.** Same shield as idle, swung on hit. **P1 / vs_left / left court** uses `*_left` frames (facing right, toward the opponent).
10. **Right-court frames from left + vs_right.** Once **all three left sprites** and **`vs_right`** exist, build `{name}_game_idle_right.png`, `{name}_game_walk_right.png`, `{name}_game_hit_right.png` **in bitmap code**. Do **not** generate them. Pipeline: each packed `*_left` → **horizontal flip** → **recolor clothes to vs_right** (hair and skin unchanged). Same **192×233** box. Right side of the court.

Idle prompt (two image refs: **Ash idle or Rivet idle**, then **A**):

```
TWO REFERENCES. (1) Court idle: COPY ONLY body math and cartoon face — bulky chibi, head about a third of the sprite, big cartoon eyes, short thick muscular torso, stubby legs, oversized boots. Do NOT copy that idle’s hair, clothes, or weapon. (2) Full-body A: COPY ONLY clothes, hair, beard, shield. Do NOT copy A’s adult anatomy or A’s small realistic eyes. FACING RIGHT (nose right, shield on the right side of the image). [CHARACTER LOCK], EXACT same clothing and shield as the full-body. NOT a scaled-down full-body, NOT adult fighter legs, NOT a small head, NOT obese, NOT a round gut. IDLE. NO drop shadow. Solid magenta #FF00FF. Pixel art. Pack 192x233.
```

Walk / hit prompt (reference **idle**, not a new design):

```
Same pixel character as the idle reference, identical clothes hair and shield, only the pose changes. NO drop shadow. Full sprite visible, not cropped. Magenta #FF00FF. [WALK one foot forward in a stride | HIT swinging the shield, NO BALL, feet planted]
```

### E — Ending / congratulations full-body

Women: cheesecake victory pose. Men: arms-up champion pose, still fully muscled in costume.

```
4:3 ending card, left third teal embossed RAILSHOT logo wallpaper, right two-thirds cyan studio backdrop, full-body 1994 arcade sprite of [CHARACTER LOCK], victory pose, empty space on the left for text, 1440x1080
```

## Generation order

Do not skip ahead: later files are derived from earlier ones.

1. **A select full-body.** Pack 480×720, chroma `#FF00FF`. **Approve A** before B.
2. **B face** from A. Pack **192×192**, no drawn border, flush left/right/bottom, chroma `#FF00FF`. **Approve A** before B.
3. **Name sprites** `{name}_name_select.png` and `{name}_name_vs.png`. Pack 480×90. No portrait input.
4. **`vs_left`.** P1 clothes matching A. Pack 480×600. **Approve vs_left** before vs_right.
5. **Left court:** `idle_left` from **Ash idle (men) or Rivet idle (women)** for body/face, plus **A** for clothes. Court idle anatomy lock. Face **right**, no shadow, pack **192×233**. Then `walk_left` and `hit_left` from idle_left. Required before right court.
6. **`vs_right` AFTER vs_left.** Bitmap flip + 2P recolor. Do not generate.
7. **Right court AFTER left sprites AND vs_right.** Bitmap flip + 2P recolor of each packed left frame. **192×233**.
8. **E ending** from that face.
9. **Courts** + **cabinet frame**.
10. **Words**, **digits**, **ball**, **chips**.
11. **Select wallpaper.** Until it exists, code draws a placeholder stamp.
12. **Wire** `Fighter.art()` paths. Missing files stay empty placeholders in UI.
13. Win / bonus / congratulations layout.

## Court floors

No fighters, no HUD, no chips. Leave **empty vertical gutters** on left and right so in-game chips sit there. No purple IC squares. No top or bottom metal rails — the cabinet supplies those.

Match **court + cabinet + chips** load from the **2P / rival**. Files: `art/{name}/{name}_court.png` (opaque 1440×1080), `art/{name}/{name}_ui_cabinet.png` (keyed 1440×1080, exact wells), and four gates `art/{name}/{name}_ui_chip_{you,cpu}.png` plus `..._down.png`. **Approve each fighter’s pair** (then that fighter’s chips) before generating the next. Missing files fall back to Rivet.

| Fighter | Floor (playbook prompt) | Court file | Cabinet file |
| --- | --- | --- | --- |
| Rivet | Circuit stadium | `rivet/rivet_court.png` | `rivet/rivet_ui_cabinet.png` |
| Ash | Parking garage | `ash/ash_court.png` | `ash/ash_ui_cabinet.png` |
| Kite | Airfield tarmac | `kite/kite_court.png` | `kite/kite_ui_cabinet.png` |
| Maru | Highland chasm | `maru/maru_court.png` | `maru/maru_ui_cabinet.png` |
| Quill | Forest aerie | `quill/quill_court.png` | `quill/quill_ui_cabinet.png` |
| Hex | Rainy neon street | `hex/hex_court.png` | `hex/hex_ui_cabinet.png` |

**Circuit stadium**

| Canvas | **1440×1080**, opaque |
| Left gutter | darker teal chip lane |
| Right gutter | darker teal chip lane |
| Vertical center | x **720** |
| Live dash size | **48×192** |
| Live dash x | **696** (`TRACE_X_PX`) |
| Dash travel (center y) | **168 → 912** |

A **live dash** rides the midline only on **Rivet’s floor** (2P). After serve wait ~2.2s, the bead ping-pongs high ↔ low in ~3.6s each way. Only that bead is solid. Fireball and ice **ricochet**. HARD’s intercept includes the bounce. Paddles ignore it. Do not bake the bead into the floor PNG.

Cabinet (`rivet/rivet_ui_cabinet.png`): authored **360×270**, nearest **×4** to 1440×1080. Left half mirrored. Chunky 8px dither, 12px traces, DIP / cap / LED / crystal / header / transistor. Gold bezels. No hazard stripes. Same well / hole table.

```
Top-down 4:3 empty sports court floor, teal circuit-board traces only, gold broken circle with X in the center, dashed center line, empty side gutters, NO purple squares, NO IC pads, NO top or bottom rails, no people, no HUD, no text, pixel art arcade background, 1440x1080
```

**Parking garage** (Ash) — **layout first, then pixels**

Do **not** freehand the wells or gutters. Author `ash_court.png` to this 1440×1080 table (`World` `COURT_GUTTER_W_PX`, `POST_W_PX`, `POST_H_PX`, `POST_X_PX`, `POST_SLOTS`):

| Piece | Pixels |
| --- | --- |
| Canvas | **1440×1080**, opaque |
| Left gutter (chip lane) | x **0…101** (dark grey, same grain as the floor, no curb) |
| Right gutter (chip lane) | x **1339…1440** (dark grey, same grain as the floor, no curb) |
| Playable floor | x **101…1339** |
| Vertical center | x **720** |
| Well size | **96×176** (thin tall rectangle) |
| High well | **(672, 152)–(768, 328)** |
| Mid well | **(672, 452)–(768, 628)** |
| Low well | **(672, 752)–(768, 928)** |

Wells are empty dark pits with a **chunky 4px steel rim** (nicked arcade pixels, not a clean CAD stroke). Walls overlay in code. No pink neon. No gold X / circle / dashed line through the pits. Gold stall paint only in open concrete. No vertical curb between the chip lanes and the floor.

```
Top-down 4:3 parking garage sports court, 1440x1080, grey concrete oil specks, gutters exactly 101px left and 101px right darker grey chip lanes, playable concrete centered, THREE thin 96x176 empty hydraulic wells at (672,152)-(768,328), (672,452)-(768,628), (672,752)-(768,928), gold stall hatch in open corners only, NO pink neon, NO magenta, NO gold X, NO dashed center line through wells, no people, no HUD, pixel art
```

**Highland chasm** (Maru)

Left and right are **cliff-tops** (playable muted sage peat). The vertical middle is a **top-down irregular chasm** looking down at water and boulders — not a gold X, not a dashed line, not a straight canal, not an isometric canyon. Sparse tufts and a few stones on the tops only. No fish, no ducklings, no pink heather carpets. Empty darker peat chip gutters.

```
Top-down 4:3 empty Scottish highland cliff-tops as a volley court, camera STRAIGHT DOWN, muted sage peat LEFT and RIGHT, sparse tufts and a few grey rocks only (NOT busy, NOT lime, NOT pink flowers), IRREGULAR chasm down the vertical middle looking down into muted dark water with a few large grey boulders at the bottom, jagged grey rock rim, empty darker peat chip gutters (no cobblestone, no curb), no fish, no ducks, no people, no HUD, no gold X, pixel art, 1440x1080
```

**Airfield tarmac** (Kite)

```
Top-down 4:3 empty airfield runway tarmac as a volley court, grey concrete asphalt landing strip (NOT grass, NOT soccer, NOT lime turf), white dashed runway centerline, threshold chevrons, gold broken circle with X, empty dark asphalt chip gutters, no planes, no people, no HUD, pixel art, 1440x1080
```

**Forest aerie** (Quill)

```
Top-down 4:3 empty woodland clearing as a volley court, CLEAN even packed earth, sparse rust leaves at the edges only (NOT muddy, NOT stained, NOT dirty scuffs, NOT neon, NOT PCB traces), gold broken circle with X, empty plain bark chip gutters, no people, no HUD, pixel art, 1440x1080
```

**Rainy neon street** (Hex) — court, cabinet, chips only; do **not** restyle Hex fighter sprites

Top-down **street canyon**: wet **empty** asphalt in the middle, dark empty **chip gutters** left and right (same dark lanes as the old Hex floor, 101px, no objects). Neon blue + neon pink only on the building faces (abstract hex / bars / chevrons, **no letters, no logos**). **No** vertical cyan/pink light beams or laser lanes down the street. Center: gold broken hex + X. Hex lamps on the inner roof corners: blue / pink / pink / blue. No baked cars (the live hovercar is overlay), no people, no HUD, no Tron grid, no Rivet PCB.

Cabinet (`hex/hex_ui_cabinet.png`): authored **360×270**, nearest **×4**, designed from scratch (not a recolor of the old hex CAD frame). Noir Blade Runner rain: wet dark steel posts, rain streaks, pipes, hex neon lamps, neon-blue LEFT / neon-pink RIGHT. Same well / hole table. P2 pink is `(255, 112, 200)` — G **> 90**. Magenta hole **1056800** px. No text. Original IP.

```
Top-down 4:3 rainy neon street canyon as a volley court, wet EMPTY dark asphalt (NO cars, NO vertical neon light beams, NO laser lanes), gold broken hexagon with X in the center, building faces with neon-blue and neon-pink abstract signs only (no text), empty dark chip gutters 101px left and right, hex lamps blue and pink, no people, no HUD, no logos, pixel art, 1440x1080
```

**Cabinet** (`art/{name}/{name}_ui_cabinet.png`) — overlay on that fighter’s court when they are 2P

The cabinet **is 4:3** (canonical **1440×1080**). It is the machine, not the court.

**Layout first.** Spec every inset in pixels on the 1440×1080 canvas. Author the sprite **on that 1440×1080 canvas, to that table** (steel rims around those rects, magenta interiors). Code uses the **same constants**. Do not generate a freeform cabinet and then reverse-engineer wells. Do **not** draw a small cabinet and scale it up. Match the in-game fighter sprites: 1994 Neo Geo / CPS1 pixels, hard outlines, dither, limited palette.

### Inset table (exclusive right / bottom)

Wells share **y = 32…100** (68px tall). Top bar, left → right:

| Well | Pixels (x0, y0)–(x1, y1) | Content (code, not baked) |
| --- | --- | --- |
| P1 score | (220, 32)–(324, 100) | Left player current-set points, two digits |
| P1 name | (340, 32)–(528, 100) | Typeset `displayName`, uppercase. **No name sprite.** |
| P1 sets | (544, 32)–(620, 100) | Two dots, 0–2 filled |
| Time | (636, 32)–(804, 100) | Countdown 99→00, two digits |
| P2 sets | (820, 32)–(896, 100) | Two dots, 0–2 filled |
| P2 name | (912, 32)–(1100, 100) | Typeset `displayName`, uppercase. **No name sprite.** |
| P2 score | (1116, 32)–(1220, 100) | Right player current-set points, two digits |
| **Playfield** | **(144, 128)–(1296, 992)** | **1152×864 = 4:3.** Court, chips, fighters, ball |

Empty wells in the PNG: magenta `#FF00FF` only. No digits, letters, or dots in the art. HUD text **must fit the well**: scale font size from well width and height (`WellText`); clip, do not overflow chrome.

**Playfield** is the only playable surface. Both cabinet and playfield are 4:3; `FillBounds` in the hole. Chrome never occludes gameplay.

`World` constants: `HOLE_*_PX`, `P1_SCORE_INSET`, `P1_NAME_INSET`, `P1_SETS_INSET`, `TIME_INSET`, `P2_SETS_INSET`, `P2_NAME_INSET`, `P2_SCORE_INSET`. `COURT_ASPECT` = 4/3.

```
4:3 arcade cabinet 1440x1080, chrome drawn around these magenta wells only: P1 score 220,32,324,100; P1 name 340,32,528,100; P1 sets 544,32,620,100; time 636,32,804,100; P2 sets 820,32,896,100; P2 name 912,32,1100,100; P2 score 1116,32,1220,100; playfield hole 144,128,1296,992 (1152x864 4:3); themed posts in side margins; bumper below y=992; no text, no digits, no dots
```

### Match, time, score, sets (code)

A **match** is **best of three sets** (first to **2** sets wins). Each set is one round of chips. After a set: freeze on the court (~0.5s) so the last flattened gate is visible, then overlay `ui_you_win.png` or `ui_you_lose.png` (~1.4s) for the P1 result, then reset chips and current-set points, fill one set dot, `ROUND`. At **2** dots, that overlay still plays, then match over.

- **Time:** TIME well is two rows — P1 **skill** on top (teal typeset, no zero-pad), clock **99 → 00** on the bottom (cream). Clock runs in `PLAYING`. At **00**, more chips remaining wins the set; if tied, next chip wins (sudden death).
- **Score:** P1 chip-hits in the **left score well**, P2 in the **right score well**. Current-set only. Skill does **not** live here.
- **Skill:** match total for P1 only, in the TIME well. Adds the popped value whenever **P1’s shot** flattens a gate (CPU rail or own-rail) and whenever a **CPU** gate falls (same events as the P1 chip well). Bank **wall / post / toy** bounces since that hit: **0 → 100**, **1 → 300**, **n ≥ 2 → 500 + (n − 2) × 200**. Ice comet uses the same table **×2**. **HARD** pays that table **×2** (EASY is ×1) — pops and the TIME well. **VS:** resets on a new match. **Arcade:** the TIME-well total is the credit score — it carries across fights until P1 loses (or clears the ladder). Then three-letter name entry if the score qualifies on that dip (EASY or HARD table), then the ranking card. No best-ever outside those tables. **Every** flattened gate pops that shot’s number in the matching score-well color (P1 teal / P2 coral) with a black drop shadow, rises, and fades (~0.75s), same motion as WW2 Blitz coin pickups.
- **Names:** typeset in the name wells. Never `{name}_name_*.png` on the cabinet.
- **Sets:** two dots per sets well. Match ends at two filled dots.

Round result overlay on a set (`Phase.SET_WIN`): `YOU WIN` if P1 took the set, `YOU LOSE` if the CPU did. Match then the **result card** (not a game ending). Full-screen `ui_select_bg.png`. Right rail: winner `{name}_ending.png` at **full stage height**. Left rail: `{name}_wins.png` **right-aligned** and **vertically centered** on the stage, plus typeset gold **CONTINUE** with chevrons **lower on the card**, **centered in the left half**. Ignore taps for **0.8s**, then tap (or **10s** idle). **VS:** → **title**. **Arcade:** win → next VS. **Beat the ladder** or **lose** → name entry if the credit score qualifies on the current EASY/HARD table, else the ranking card. Do not staff-roll or bonus-tally here.

Each set starts in `ROUND`: draw `ui_round.png` plus `ui_num_{1,2,3}.png` for ~1.8s, then `SERVE` with `ui_fight.png` for ~1.2s (it clears; the set stays on serve). Tap the playfield to serve (does not move the shield). Drag only starts if the thumb lands on P1’s sprite (fat-finger slop, grab offset held so the fighter does not teleport under the pad). Double-tap empty court in `PLAYING` (one thumb or a second thumb while holding the rail) calls that fighter’s **once-per-set** shot at a living gate near the second tap. Serve taps do not count. CPU HARD calls the same shot after three returns. Do not typeset FIGHT or TAP TO SERVE.

**CPU** loads from the **2P fighter**. **Wall** (Maru, Hex): camps a living gate, smaller motion. **Slugger** (Rivet, Ash, Kite, Quill): chases, overcommits. **Difficulty** is set on **title** (EASY/HARD). Easy abandons high/low rails and reacts late. Hard predicts the bounce and cuts the line.

**Fighter kits** (P1 and 2P, code): same sport, readable knobs. Rally creeps speed each paddle bounce (capped). HARD reacts earlier and closes faster. Signatures fire from the hit you already do — no extra button. Rivet stacks pop (hotter clang, at least a medium tail). Ash smash-swipe locks a long tail. Kite afterburn locks a long tail. Maru center dump locks a short tail and a dull clang. Quill dive rims lock a long tail. Court sprites **never** scale with hitbox — packed 192×233 at baseline paddle height for every fighter, including Hex.

## In-game extras

Use section **D** for player sprites. Double-tap empty court calls **one ice comet** per set per side (`art/ui_ice_ball.png` plus ice tails), same size and flicker as the fireball, cyan instead of orange fire. From the fighter toward an upright opponent gate near the tap. Quill still prefers the high or low half. The orb **does not pass shields**: a paddle smash destroys it (shield flash, `sfx_ice.wav`) while the fireball keeps moving. Death overlays `ui_ice_burst_{a,b,c}.png` at the orb for ~0.24s, gate flatten or shield smash. If it gets through, it flattens **whichever living gate it hits** (a wall bounce can send it into your own rail), plays chip SFX, and uses the same cabinet sting as the fireball. CPU chases the nearer incoming shot.

A **court wall** comes out of a **mid-line well** only on **Ash’s floor** (the court loaded from 2P). Other courts stay empty. The camera is top-down: do **not** grow the sprite up the screen. Frames `ash/ash_court_wall_{a,b,c}.png` play inside the well toward the viewer. Three wells on the center line; as one **sinks**, the next **raises** (no empty rest). After serve wait ~2.2s, then high / mid / low in order. Hold ~3.4s at full. Fireball and ice comet **ricochet** like a wall (`sfx_wall.wav`) — no freeze, no chip, no paddle lock. HARD’s intercept includes that bounce. Paddles ignore it.

A **live trace** rides Rivet’s midline (`art/rivet/rivet_court_trace.png`, **48×192**, magenta `#FF00FF`). Gold-plated DIP / memory chip (ceramic body, gold lid, pins on both long sides, notch), same language as Rivet’s life chips — not a teal copper dash. Same ricochet / HARD rules as Ash’s wall. Other courts stay empty of this bead.

A **live hovercar** rides Hex’s street (`art/hex/hex_car.png`, keyed magenta `#FF00FF`). On **Hex’s floor** (2P) it enters from the bottom of the playfield, pauses on the gold X, then drives off through the top, and loops. Clip to the court hole. Fireball and ice comet **ricochet** off it while it is on screen (`sfx_wall.wav`) — no freeze, no chip, no paddle lock. HARD’s intercept includes that bounce. Paddles ignore it. Other courts stay empty of this car.

A **live hawk** rides Quill’s aerie (`art/quill/quill_court_hawk_{a,c,b}.png`, keyed magenta `#FF00FF`, packed **136×192**). On **Quill’s floor** (2P) it loops a **rounded square** around the playfield: high and low rails plus the lanes **in front of both fighters** (inset so it does not graze the paddles). Beak follows the path; corners rotate smoothly. Wingbeat ping-pongs open / mid / tucked. Never the gold X. No pause. Clip to the court hole. Fireball and ice comet **ricochet** (`sfx_wall.wav`) — no freeze, no chip, no paddle lock. HARD’s intercept includes that bounce. Paddles ignore it. Other courts stay empty of this hawk.

A **live gold X** rides Kite’s tarmac (`art/kite/kite_court_x.png`, keyed magenta `#FF00FF`, packed **192×192**, dest **156×156**). On **Kite’s floor** (2P) it sits on the painted X (`World` `KITE_X_CX_PX` **719**, `KITE_X_CY_PX` **529** — the floor mark is high of geometric center) and **spins in place** (~10s per turn). Bounce off whichever arm you hit (`sfx_wall.wav`) — no freeze, no chip, no paddle lock. HARD’s intercept includes that bounce. Paddles ignore it. Other courts stay empty of this X.

A **live log** rides Maru’s chasm (`art/maru/maru_court_log.png`, keyed magenta `#FF00FF`, packed **240×40**, dest long×thick along the current — mixed sizes around **80–134 × 24–41**). On **Maru’s floor** (2P) **three** copies drift **down the water** (high → low, wrap), **uneven gaps**, long axis with the flow. They **weave around the painted boulders** (right of the high rock, left of the mid rock, right of the low rock, left of the bottom rock). Ricochet (`sfx_wall.wav`) — no freeze, no chip, no paddle lock. HARD’s intercept includes that bounce. Paddles ignore it. Other courts stay empty of these logs.

**Ice burst** (`art/ui_ice_burst_a.png`, `_b`, `_c`)

Three keyed ice-shatter frames, 128×128. Overlay when the ice comet dies. Magenta `#FF00FF`.

**Court wall** (`art/ash/ash_court_wall_a.png`, `_b`, `_c`)

Keyed top-down hazard plate, packed **96×176** (same rect as a well). Author **24×44**, nearest **×4**. Exact magenta `#FF00FF` field. Diagonal cabinet stripes: **exactly 2 yellow + 2 dark** on every frame (A/B/C), 2×2 dither, steel rim, four corner bolts. Limited palette, no anti-alias. **A** peeking, **B** mid-raise, **C** fills the well. Draw dest is the **full well rect**.

**Mid-line wells** — Ash court only. Same table as **Parking garage** above. `World`: `COURT_GUTTER_W_PX` 101, `POST_W_PX` 96, `POST_H_PX` 176, `POST_X_PX` 672, `POST_SLOTS` 240 / 540 / 840. Do not bake wells into the other floors.

**Ice ball** (`art/ui_ice_ball.png`)

Same 60×60 keyed sphere as the play-ball, ice-blue / cyan instead of steel-gold. No fire. Magenta `#FF00FF`.

**Ice tail** (six files, same pockets as the fire tails)

Same tongue layouts as `ui_ball_tail_*`, recolored cyan ice. Flicker and heading match the fireball draw.

| Length | Flicker A | Flicker B |
| --- | --- | --- |
| Short (slow) | `ui_ice_ball_tail_short_left.png` | `ui_ice_ball_tail_short_right.png` |
| Medium | `ui_ice_ball_tail_medium_left.png` | `ui_ice_ball_tail_medium_right.png` |
| Long (fast) | `ui_ice_ball_tail_long_left.png` | `ui_ice_ball_tail_long_right.png` |

**Ball** (`art/ui_ball.png`)

Bright **steel sphere** with gold bounce from the fire — 1994 arcade hard bands and dither, **no dark hemisphere**, **no baked outline** (the tail pocket is the rim). No flame on this file. Do not bake a comet or halo. Do not rotate the orb.

```
60x60 bright steel sphere, 1994 Neo Geo pixel art, hard bands and dithering not smooth gradients, white highlight, light steel and gold bounce, NO dark shadows, NO outline (the tail sprites hug the orb), no fire halo, no tail, magenta background #FF00FF
```

**Ball tail** (six files, keyed `#FF00FF`)

A straight **tight** fire cone that wraps the orb then tapers behind it. Tongues hug the axis — not a thin sperm-streak, not a bushy mane/beard of radial spikes, not a left/right wag.

| Length | Flicker A | Flicker B |
| --- | --- | --- |
| Short (slow) | `ui_ball_tail_short_left.png` | `ui_ball_tail_short_right.png` |
| Medium | `ui_ball_tail_medium_left.png` | `ui_ball_tail_medium_right.png` |
| Long (fast) | `ui_ball_tail_long_left.png` | `ui_ball_tail_long_right.png` |

Rules:

1. **Complete tongues, no box crop.** The metal orb sits in a circular KEY pocket. Fire wraps that pocket, then the cone trails left. **Never crop** a flame to a rectangle, a circle, or a shorter length — chopped edges (horizontal *or* top/bottom) are wrong. Pack the full bbox of every tongue.
2. **Two flicker poses, same tight cone.** `_left` / `_right` are **two different tongue layouts** (chips, wrap, embers) of the same axis-hugging cone — not a mirror, not a lean, and not a thin streak vs a burst.
3. **Three lengths, complete flames.** Short / medium / long are separate PNGs. Each sprite contains the **full flame** from the wrap to a **natural tapering tip** (embers, dying tongues). Do not scale one sprite to fake speed. Draw scales the pocket to the ball; extra wrap is allowed to show.
4. **Velocity bands.** Split the live speed range `[BALL_SPEED × SLICE_CENTER, BALL_SPEED × SLICE_CAP]` into three equal segments. Slow → short pair, medium → medium pair, high → long pair. Draw **no tail** when the ball is parked / serving (speed ≈ 0).
5. **Flicker.** While that band is active, alternate its left/right file as the ball travels (distance-based, not wall-clock). When speed crosses a band, **switch in** that band’s left/right pair.
6. **Heading.** Canvas-rotate the current tail around the ball center so the cone points opposite velocity. Pocket / attach is the same on every file: KEY hole on the **right**, cone to the **left**. Magenta `#FF00FF`. Draw maps that hole to the ball; do not squash the sprite to a 128px box.

**Life chips** (gutter **gates**, two states)

Behind each fighter. Theme matches the **2P court**, not a global steel set. **Upright:** a thick vertical bar on the court-facing edge of the gutter (the gate edge-on, blocking the ball) — **no bolts** (they read as noise on a phone). **Flattened:** the gate has fallen onto its back — a **plain plate** filling the gutter slot, **four medium gold bolts** (one per corner), thin team accent, no stripes, no lamp cluster, no bolt grid. P1 teal (or that court’s P1 accent), P2 crimson. P2 is flipped in code. No numerals. Dead gates still draw (flattened); they do not collide.

Files: `{name}/{name}_ui_chip_you.png`, `{name}/{name}_ui_chip_cpu.png`, `{name}/{name}_ui_chip_you_down.png`, `{name}/{name}_ui_chip_cpu_down.png`. Standing **22×120**, down **48×120**. Magenta `#FF00FF` if the bar does not fill the canvas.

| State | P1 | P2 |
| --- | --- | --- |
| Standing | `{name}_ui_chip_you.png` | `{name}_ui_chip_cpu.png` |
| Flattened | `{name}_ui_chip_you_down.png` | `{name}_ui_chip_cpu_down.png` |

**Approve each fighter’s four chips** before the next fighter. Missing files fall back to Rivet. Gates must **read on that court’s empty side bands**: lighter or darker than the gutter, hard 1px ink, team seam. Do not paint them the same color as the gutter.

Rivet (ceramic gold DIP):

```
1994 Neo Geo sprite, chunky pixels (author small, nearest scale), hard black outline. Ceramic brown body, GOLD plated lid with a left highlight (no die window, no waffle dither, not a DIMM or memory stick). Standing 22×120: DIP face-on, vertical, pin row toward midcourt, thin TEAL pip. Flattened 48×120: classic DIP on its back, gold lid, pins on BOTH long sides. NO text, NO four steel bolts, NO hazard stripes.
```

P2: same drawing, crimson pip instead of teal.

Ash (parking stall):

```
1994 Neo Geo sprite, chunky pixels (author small, nearest scale), hard black outline. CONCRETE bay with GOLD stall hatch (2 gold + 2 concrete diagonals, same language as the floor corners). Standing 22×120: stall seen edge-on, hatch on the face, thin TEAL curb toward midcourt. Flattened 48×120: top-down parking bay, hatch inside yellow stall lines, TEAL curb at the stall head. NO steel plate, NO four corner bolts, NO black hazard (that's the cabinet / well wall). No text.
```

P2: same drawing, crimson curb instead of teal.

Hex (wet metal gate):

```
1994 Neo Geo sprite, chunky pixels (author small, nearest scale). Wet WORN metal (rain pits, scratches), slightly lighter than Hex gutters. Hard dark ink rim (NOT a neon frame). Four NEON STUDS in the corners (blue P1 / pink P2), not thin neon outlines. Standing 22×120: that metal bar, four corner studs, a SINGLE vertical neon light down the CENTER only. Flattened 48×120: FULL SLOT worn-metal plate, four corner neon studs, no center stripe, no gold hex, no yellow X. NO crystal, NO letters, NO logos, NO four gold bolts, NO teal.
```

P2: same drawing, neon pink instead of neon blue.

Maru (carved stone gate):

```
1994 Neo Geo sprite, chunky pixels (author small, nearest scale), hard black outline. Same as the cabinet TOP CORNER plates: cracked brown stone, chunky aged gold rim, large round gold rivet in the center, small round gold rivets in the four corners. Shape is a RECTANGLE not a square (rivets stay circular, do not stretch). Standing 22x120: that plate edge-on, gold face toward midcourt, NO rivet. Flattened 48x120: FULL SLOT rectangular plate, gold rim, complete center rivet plus four small corner rivets. NO ice-blue, NO crimson, NO tartan, NO magenta, NO text.
```

P2: same drawing as P1 (no team pip).

Kite (olive airfield cabinet):

```
Same gate layout as Rivet. Standing: thick vertical OLIVE drab steel bar, NO bolts, TEAL seam, not grass. Flattened: olive plate, FOUR MEDIUM gold bolts, thin TEAL top and bottom. No jets, no turf. P2 crimson seam.
```

## Word and banner sprites

One word per image, huge, centered, magenta background.

```
Arcade pixel word ROUND, fat brush letters, red-to-yellow horizontal gradient, thick blue outline, white highlight, 1994 Neo Geo title, no other text, magenta background #FF00FF, 960 wide
```

File: `art/ui_round.png`. Show at the start of each set with `art/ui_num_{1,2,3}.png` under it (~1.8s), then `FIGHT`.

```
Arcade pixel word FIGHT, fat brush letters, red-to-yellow horizontal gradient, thick blue outline, white highlight, 1994 Neo Geo title like ROUND, no other text, magenta background #FF00FF, 960 wide
```

File: `art/ui_fight.png`. Serve sting ~1.2s, then the court waits empty. No typeset TAP TO SERVE. Tap still serves.

```
Arcade pixel words YOU WIN, huge brush letters, yellow-orange-red gradient, blue outline, magenta background, 960 wide
```

File: `art/ui_you_win.png`. P1 took the set.

```
Arcade pixel words YOU LOSE, huge brush letters, icy blue-to-purple gradient, orange outline, magenta background, 960 wide
```

File: `art/ui_you_lose.png`. CPU took the set.

After the last gate of a set flattens: hold the court 0.5s with no banner, then the matching overlay 1.4s, then next `ROUND` or the ending still.
```
Arcade pixel letters VS, overlapping block capitals V and S (not script, not italic), gold chrome fill, orange outline, magenta #FF00FF, 384x384
```

File: `art/ui_vs.png`. No drop shadow. Rewrite field to `#FF00FF`.

```
Arcade pixel word SELECT, fat brush letters, red-to-yellow horizontal gradient, thick blue outline, white highlight, 1994 Neo Geo title like ROUND, magenta #FF00FF, 640x220
```

File: `art/ui_select.png`.

```
Arcade pixel word FIGHTER, fat brush letters, red-to-yellow horizontal gradient, thick blue outline, white highlight, 1994 Neo Geo title like ROUND, magenta #FF00FF, 640x220
```

File: `art/ui_fighter.png`. Stacked on player select under SELECT. Keep `art/ui_player_select.png` unused for revert.

```
Pixel digits 0-9 sheet, gold with blue outline, 120x150 each, for TIME and SCORE, magenta background
```

Bonus tally labels:

```
Pixel words TARGET, PERFECT, STRAIGHT, TOTAL, blue-to-yellow gradient, outlined, one row, magenta background
```

## Cabinet loop

Two separate screens. Do **not** composite the CPU match under or over the title.

1. **Title.** Composited **in code** on `ui_select_bg.png`: select full-bodies **Ash, Kite, Rivet** on the left (flipped to face center); **Hex, Quill, Maru** on the right, with a little top/side margin. **V stagger:** Ash and Maru highest, Kite and Quill a step lower, Rivet and Hex lowest (and in front). **RAIL** and **SHOT** (`ui_rail.png`, `ui_shot.png`, 640×220, magenta `#FF00FF`) sit **on one line at the top**, **horizontally centered** in the gap between Ash and Maru. Bottom menu, typeset gold arcade font (`fonts/arcade_font.ttf`): top row **ARCADE** **VS** side by side; under them **EASY** or **HARD** (the live dip). No square brackets — these are not submenu entries. Chevrons default to **VS**; the touched item takes them. After **ARCADE** or **VS**, linger **1 second** so the chevrons are visible, then go to character select. Pressing EASY/HARD swaps the dip (and gameplay) and **restarts the 5s demo timer**. Do **not** blit `ui_btn_*.png` / `ui_arrow_left.png` (sprites stay in `art/` unused). Hold **5 seconds** with no press, then go to demo. Do not typeset RAIL or SHOT. Do not tap-anywhere-to-start. Do **not** bake a `ui_title.png` lineup. No difficulty arrows on title.
2. **Demo.** A **CPU vs CPU** match for **10 seconds** (random roster pair each time, mirrors allowed, **HARD** on both rails, no SFX / announcer). Typeset **DEMO** in smaller red letters in the **center of the court**, flashing, no black outline. Tap anywhere → **title** (mode buttons).
3. **Ranking.** After demo, **TOP SCORES** for the **live title dip** (EASY table or HARD table), 10 rows, ~4 seconds, then back to title. Smaller title / dip / row type with open vertical air. Tap anywhere → **title**. Same card after arcade name entry (or a credit that did not qualify). Blink **1P START**. Do not mix EASY scores onto HARD. Tables live in `arcade_leaderboard` prefs **and** `filesDir/arcade_leaderboard.eeprom` (Blitz EEPROM shape, `commit` + file sync). They survive process death and APK updates. Uninstall still wipes them (`allowBackup=false`, same as Blitz).

**Select idle.** If nobody touches the screen for **45 seconds** while picking, return to **title** (cabinet that never got a confirm). Any tap on select resets the timer. After both fighters are locked (linger into VS), do **not** idle-kick.

## Character select layout

4:3 letterboxed screen. Use pieces **A** (full-body), **B** (face), **F** (name), plus `ui_select.png` / `ui_fighter.png`. Overlay cursor, arrows, and SELECT in code. Wallpaper has no portraits and no UI.

Wallpaper only (`art/ui_select_bg.png`): quiet dark teal stage, no portraits, no UI, not busy.

```
4:3 quiet arcade select wallpaper, dark teal navy gradient, sparse dither, no people, no text, no tiles, 1440x1080
```

**Name sprites** (piece **F**, two files per fighter)

Portraits are **not** inputs. Pack each to **480×90**, magenta `#FF00FF`.

**Select name** `{name}_name_select.png` — letters only, same 90s fighter-name lettering as the VS names (no chip, no plate):

```
Arcade fighter name [NAME], fat Neo Geo capitals, gold-yellow fill, blue outline, white left highlight, LETTERS ONLY no box no plate, 480x90, magenta #FF00FF
```

Use on **player select**, left column, under `PLAYER SELECT`.

**VS word** `{name}_name_vs.png` — letters only, blue with white edge:

```
Arcade word [NAME], fat blue capitals, white left highlight, dark outline, no gold, no box, no character, 480x90, magenta #FF00FF
```

Use on **VS**, under each bust. Missing file: placeholder.

### Select screen rules (code)

Same flow as a 90s arcade vs-select (Flip Shot layout language, original roster):

1. **Roster strip** along the **bottom**, **full 4:3 width**: six face tiles in title order **Ash, Kite, Rivet, Hex, Quill, Maru**. Missing **B** art is an empty framed placeholder with the name (or initial), same tile size.
2. **Browse with on-screen arrows.** Arrows sit **left and right of the SELECT button**. They move the roster cursor. Wrap at the ends. Arrows do not confirm.
3. **Confirm with SELECT.** The SELECT button (between the arrows, left stack) locks the highlighted fighter. **VS mode:** first press = **first fighter** (left court, `vs_left`, P1 clothes); second press = **second fighter** (right court, `vs_right`, 2P clothes). Same fighter twice is a legal mirror match. **Arcade mode** (from the title **ARCADE** button): one SELECT locks 1P only; first CPU is the **next fighter** after 1P in select/title order (**Ash, Kite, Rivet, Hex, Quill, Maru**; Maru wraps to Ash). No 2P cursor. Linger into VS. **Win** → next VS vs the next fighter in that order (skip 1P). Skill in the TIME well **carries** between those fights. **Beat every other fighter** or **lose** → name entry (tap left/right to spin A–Z, tap bottom to lock each of three letters) if the credit qualifies on the current dip, then ranking. Title EASY/HARD applies to the whole arcade run and picks the ranking table.
4. **Full-body on the right.** Piece **A** for the **currently highlighted** tile is drawn on the **right**, **almost the full 4:3 height** (`FillHeight`, no crop-zoom of the face), with a **small margin** top and bottom so it does not clip the screen edge. A must be the **left-facing** select pose so they look toward the left stack. Missing **A** is an empty placeholder. Switching left/right updates this portrait immediately. Do not keep showing the already-locked first fighter while browsing for the second.
5. **Cursor badges.** While picking the first fighter, show **1P** on the highlighted tile. After the first lock, **1P** stays on that tile; **2P** rides the cursor until the second lock. **Arcade:** **1P** only — never **2P**. Tile frames: drawn arcade rectangles (idle grey; blink gold when that tile is the cursor). Keep `ui_face_frame_*.png` on disk unused.
6. **Left stack.** Top to bottom: `ui_select.png` then `ui_fighter.png`, then `{name}_name_select.png`, then one **typeset flavor line** (`FighterKit.flavor`) for the highlighted fighter, then typeset gold **L** **SELECT** **R** with chevrons on the last touched control. All on the **left**. Do not typeset SELECT FIGHTER or the fighter name. Do not bake the flavor into piece A.
7. **Arcade type, not chrome buttons.** Menu labels use the yellow Blitz arcade font + chevrons. Cursor badges stay `ui_1p.png` / `ui_2p.png`. Button/arrow PNGs stay in `art/` for revert.
8. **After both are selected:** freeze input, linger **1.5 seconds**, then go to the **VS screen**. Do not skip the linger.

## VS screen layout

Use pieces **C** and **F** plus `ui_vs.png`. Composite two busts + name sprites + VS mark in code. Wallpaper has no portraits.

Wallpaper (`art/ui_vs_bg.png`): quiet dark navy with a faint center glow for the VS mark. No portraits, no letters, not busy.

```
4:3 quiet arcade VS wallpaper, dark navy vignette, faint warm center glow, no people, no text, 1440x1080
```

### VS screen rules (code)

1. **Left slot** = first selected fighter’s `{name}_vs_left.png` (P1 clothes, facing the center).
2. **Right slot** = second selected fighter’s `{name}_vs_right.png` (2P clothes, facing the center).
3. Missing **C** art is an empty bust placeholder in that slot (same box).
4. Draw `ui_vs.png` in the center. Under each bust draw `{name}_name_vs.png` (first pick left, second pick right). Missing name sprite: placeholder.
5. Bottom bar is two rows: fighter `{name}_name_vs.png` plates on the first row (large, under each bust), then a gap, then typeset gold **START** with chevrons centered on the second row. Pressing START starts the match (select confirm SFX) at the title difficulty. No EASY/HARD on this screen. Busts are unframed (arcade rectangles are select-tile only).
6. Stay on VS until START is pressed. Left court then uses the first fighter’s `*_left` anims; right court uses the second’s `*_right` anims.
7. **Intro:** START is on from frame one. Busts (and their name plates) slide in from the left/right edges. After they land, `ui_vs.png` scales up from zero at its resting center.

## Win / bonus / congratulations layout

Round win in-court is the `YOU WIN` / `YOU LOSE` overlay when a **set** is taken (first to **2** of **3** sets wins the match). Use piece **E** for the match champion. Other cards:

**Bonus tally**

```
4:3 arcade results screen, large bust of [CHARACTER LOCK] on a white background, empty right side for score numbers, 1994 pixel portrait, no text, 1440x1080
```

**You are No.1** — same as **E**, leave left side empty for title text.

**Staff / group ending**

```
4:3 sepia pixel group photo of six original Railshot fighters posed together against a brick wall, 1994 arcade ending still, no real-world people, 1440x840 with black letterbox
```

## What we overlay in code (do not bake in)

- P1 score in the left score well, P2 score in the right score well
- Typeset names in the P1 / P2 name wells (no name sprites)
- Set dots (2 of 3) in the sets wells
- TIME well: P1 skill (top, teal) + countdown **99 → 00** (bottom, cream)
- Skill pop on every flattened gate, P1 teal / P2 coral (same as the chip-count wells)
- Six gutter gates per rail (standing; flattened after a hit)
- Paddles / fighters in the lane
- Ball plus speed-banded tail (`ui_ball_tail_{short,medium,long}_{left,right}.png`)
- Called ice comet plus ice tails (`ui_ice_ball.png`, `ui_ice_ball_tail_*.png`)
- Ice burst overlay (`ui_ice_burst_{a,b,c}.png`) when the comet dies
- Rising court wall (`ash/ash_court_wall_{a,b,c}.png`) in the live mid-line well on Ash’s court
- Live center dash (`rivet/rivet_court_trace.png`) on Rivet’s court
- Live street hovercar (`hex/hex_car.png`) on Hex’s court — bottom → pause on X → top, loop
- Live aerie hawk (`quill/quill_court_hawk_{a,c,b}.png`) on Quill’s court — rounded square around the rims, skip the X
- Live gold X (`kite/kite_court_x.png`) on Kite’s court — spins in place on the painted mark
- Live chasm logs (`maru/maru_court_log.png`) on Maru’s court — three mixed sizes drift and weave the water
- `ROUND` / `FIGHT` / `YOU WIN` / `YOU LOSE` banners
- Select cursor (`ui_1p.png` / `ui_2p.png`); typeset gold **L** **SELECT** **R** with chevrons
- Title typeset **ARCADE** **VS** on one row, **EASY** or **HARD** under them (arcade font, yellow, chevrons on touch, no square brackets); **RAIL** **SHOT** on one line at the top (`ui_rail.png` / `ui_shot.png`); arcade is a roster ladder (skip 1P)
- Title difficulty swaps EASY↔HARD; VS typeset **START** begins the match; result typeset **CONTINUE**
- Attract ranking **TOP SCORES** (EASY or HARD table) after demo; arcade name entry then the same card
- Select flavor line under the name sprite (`FighterKit`)
- Linger-then-VS transition
- Tally numbers

Courts bake the three mid-line wells. Portraits stay without UI text so we can localize and animate later.
