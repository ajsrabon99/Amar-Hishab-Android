import subprocess
import os

# Generate exact official Amar Hishab icon
# Background: Royal Navy gradient (#263E77 -> #1A2B58 -> #0F1A38)
# Foreground: Stylized 'A' glyph with top-right diagonal notch and inner archway incision

svg_content = '''<?xml version="1.0" encoding="utf-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">
  <defs>
    <linearGradient id="bgGradient" x1="15%" y1="10%" x2="85%" y2="90%">
      <stop offset="0%" stop-color="#284482" />
      <stop offset="45%" stop-color="#1E3166" />
      <stop offset="100%" stop-color="#101B3A" />
    </linearGradient>
  </defs>

  <!-- Rounded Squircle Background -->
  <rect x="0" y="0" width="1024" height="1024" rx="225" fill="url(#bgGradient)" />

  <!-- White Stylized A Glyph -->
  <!-- 1. Left stem and upper apex body -->
  <!-- Top apex has rounded top-left, flat top, and diagonal cut on top-right -->
  <path fill="#FFFFFF" fill-rule="evenodd" d="
    M 470,165 
    C 440,165 415,190 395,235 
    L 165,770 
    C 145,815 170,845 220,845 
    L 310,845 
    C 345,845 365,825 380,785 
    L 410,705 
    C 415,690 425,675 440,650
    C 445,630 460,545 460,530
    C 460,510 475,495 500,495
    L 535,495
    C 555,495 570,510 570,530
    L 570,570
    C 570,590 585,605 605,605
    L 635,605
    L 520,340
    C 505,305 500,270 515,230
    L 535,185
    C 540,175 530,165 520,165
    Z" />

</svg>
'''
print("make_official_logo.py written")
