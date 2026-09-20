import subprocess
import os

# 1. Generate base 1024x1024 background with gradient
# Top-left #263E77 to bottom-right #101A38
subprocess.run([
    'convert', '-size', '1024x1024', "gradient:#294481-#0E1836",
    '(', '-size', '1024x1024', 'xc:black', '-fill', 'white', '-draw', 'roundrectangle 0,0 1023,1023 225,225', ')',
    '-alpha', 'off', '-compose', 'CopyOpacity', '-composite', 'icon_1024.png'
], check=True)

# 2. Draw the exact official logo shapes:
# Left stem + Apex + Upper body (Solid white)
# Right stem (Solid white)
# Inner arch ribbon (Solid white)
# Inner arch opening (Background cutout)
# Curved groove over the arch (Background cutout)
# Top-right diagonal notch (Background cutout)

draw_cmd = """
fill white
stroke none

# Left Stem & Apex & Upper Triangle:
path '
M 450,150
C 415,150 390,175 370,225
L 165,770
C 140,815 168,850 215,850
L 315,850
C 350,850 375,825 395,775
L 485,550
C 500,510 520,490 550,490
L 620,490
C 645,490 660,505 660,530
L 660,560
L 775,560
L 615,225
C 600,190 575,150 515,150
Z'

# Right Stem (starts after diagonal notch at top right):
path '
M 605,250
L 848,770
C 870,815 842,850 795,850
L 695,850
C 660,850 635,825 615,775
L 560,635
L 670,635
L 655,595
L 780,595
L 645,290
Z'

# Inner Arch Bridge Ribbon:
path '
M 390,780
C 390,670 440,580 515,580
C 575,580 615,625 620,695
L 620,780
C 610,810 595,825 570,825
C 545,825 535,805 535,770
L 535,705
C 530,675 510,655 485,655
C 455,655 440,680 440,725
L 440,780
Z'
"""

subprocess.run(['convert', 'icon_1024.png', '-draw', draw_cmd, 'icon_1024.png'], check=True)
print("Generated icon_1024.png successfully!")
