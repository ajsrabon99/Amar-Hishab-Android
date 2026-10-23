#!/bin/bash
# Crop the right landscape from reference screenshot: x=260 to 665, y=0 to 360
convert public/assets/aistudio/Screenshot_2026-10-22-10-31-10-546_com.openai.chatgpt_99fa0b2f.png -crop 405x360+260+0 right_nature.png

# The eye icon is at x=325, y=65 (relative to x=260) with radius 36
# Sample the sky color around it and paint a smooth patch matching the ambient sky
convert right_nature.png \
  \( -size 80x80 xc:'#0a2b48' -blur 0x15 \) -geometry +285+25 -composite \
  right_clean.png

convert right_clean.png -resize 600x420 right_clean_scaled.png
