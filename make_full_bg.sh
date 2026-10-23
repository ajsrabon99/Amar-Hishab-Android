#!/bin/bash
W=665
H=531

# 1. Base background: deep navy gradient matching the exact palette of the card
convert -size ${W}x${H} -define gradient:direction=north gradient:'#030a16'-'#071b32' full_base.png

# 2. Extract right landscape from reference: width 400, height 360, x=265, y=0
convert public/assets/aistudio/Screenshot_2026-10-22-10-31-10-546_com.openai.chatgpt_99fa0b2f.png -crop 400x360+265+0 right_landscape.png

# 3. Create a soft feathered mask for blending left edge of the right landscape
convert -size 400x360 gradient:black-white -rotate 90 mask.png

# 4. Inpaint the eye icon area on right_landscape (x: 260 to 350, y: 30 to 110) with matching sky
# The sky around it has color #061c33 to #092642
convert right_landscape.png \
  \( -size 80x80 xc:'#08223a' -blur 0x12 \) -geometry +285+25 -composite \
  right_landscape_clean.png

# 5. Composite right landscape onto base with soft feathering on the left
convert right_landscape_clean.png \( -size 400x360 -define gradient:direction=east gradient:black-white -level 0,70% \) \
  -alpha off -compose CopyOpacity -composite right_feathered.png

convert full_base.png right_feathered.png -geometry +265+0 -composite card_bg_test.png

ls -lh card_bg_test.png
