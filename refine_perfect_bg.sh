#!/bin/bash
W=665
H=531

# 1. Base background: deep dark midnight gradient matching the screenshot palette perfectly
convert -size ${W}x${H} -define gradient:direction=north gradient:'#030914'-'#061a32' full_base.png

# 2. Extract right landscape starting from x=315 (strictly NO digits or text)
# Original screenshot width is 665, so 665-315 = 350
convert public/assets/aistudio/Screenshot_2026-10-22-10-31-10-546_com.openai.chatgpt_99fa0b2f.png -crop 350x280+315+0 right_landscape.png

# 3. Clean eye icon area: in this 350x280 crop, eye circle center is at x=268, y=65
# Sample the clean night sky above moon (x=160, y=25, 80x80) and blur-composite over it
convert right_landscape.png \
  \( right_landscape.png -crop 80x80+160+25 -blur 0x8 \) -geometry +240+25 -composite \
  right_clean.png

# 4. Soft feather mask (left edge 80px, bottom edge 50px)
convert -size 350x280 xc:white \
  \( -size 80x280 -define gradient:direction=east gradient:black-white \) -geometry +0+0 -composite \
  \( -size 350x50 -define gradient:direction=south gradient:white-black \) -geometry +0+230 -composite \
  mask.png

convert right_clean.png mask.png -alpha off -compose CopyOpacity -composite right_feathered.png

# 5. Composite onto base at x=315
convert full_base.png right_feathered.png -geometry +315+0 -composite perfect_landscape_bg.png

cp perfect_landscape_bg.png app/src/main/res/drawable/bg_balance_card_landscape.png
ls -lh app/src/main/res/drawable/bg_balance_card_landscape.png
