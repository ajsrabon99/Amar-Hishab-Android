#!/bin/bash
W=665
H=531

# 1. Base background: deep navy gradient matching the card's exact night palette
convert -size ${W}x${H} -define gradient:direction=north gradient:'#030914'-'#061a32' full_base.png

# 2. Extract right landscape without the cards: width 410, height 275 (only sky, moon, mountains, lake)
convert public/assets/aistudio/Screenshot_2026-10-22-10-31-10-546_com.openai.chatgpt_99fa0b2f.png -crop 410x275+255+0 right_landscape.png

# 3. Seamlessly repair the eye icon area:
# In right_landscape (width 410, height 275), the eye circle is at x=330, y=65 (radius 36)
# The sky around it has a smooth radial gradient. Let's patch it with a feathered sky sample from x=210, y=20 (width 80, height 80)
convert right_landscape.png \( right_landscape.png -crop 80x80+210+25 -blur 0x3 \) -geometry +290+25 -composite right_landscape_clean.png

# 4. Create horizontal and vertical feather masks
# Left feather (fade in from x=0 to 120 of the right_landscape)
# Bottom feather (fade out at y=230 to 275 of the right_landscape into the deep lake/navy base)
convert -size 410x275 xc:white \
  \( -size 120x275 -define gradient:direction=east gradient:black-white \) -geometry +0+0 -composite \
  \( -size 410x55 -define gradient:direction=south gradient:white-black \) -geometry +0+220 -composite \
  mask_2d.png

convert right_landscape_clean.png mask_2d.png -alpha off -compose CopyOpacity -composite right_feathered.png

# 5. Composite feathered landscape onto the full_base
convert full_base.png right_feathered.png -geometry +255+0 -composite bg_balance_card_landscape.png

cp bg_balance_card_landscape.png app/src/main/res/drawable/bg_balance_card_landscape.png
ls -lh app/src/main/res/drawable/bg_balance_card_landscape.png
