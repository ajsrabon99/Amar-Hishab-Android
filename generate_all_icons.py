import subprocess
import os

# 1. Generate full square/squircle icon_1024.png and round icon_round_1024.png
subprocess.run([
    'convert', '-size', '1024x1024', "gradient:#294481-#0E1836",
    '(', '-size', '1024x1024', 'xc:black', '-fill', 'white', '-draw', 'circle 512,512 512,1', ')',
    '-alpha', 'off', '-compose', 'CopyOpacity', '-composite', 'icon_round_1024.png'
], check=True)

# Draw glyph on round icon
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

# Right Stem:
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
subprocess.run(['convert', 'icon_round_1024.png', '-draw', draw_cmd, 'icon_round_1024.png'], check=True)

# 2. Also generate transparent foreground for adaptive icons
# 108dp canvas with safe zone 72dp
subprocess.run([
    'convert', '-size', '1024x1024', 'xc:none',
    '-draw', draw_cmd, 'icon_foreground_1024.png'
], check=True)

# 3. Rescale to all mipmap directories
densities = {
    'mipmap-mdpi-v4': 48,
    'mipmap-hdpi-v4': 72,
    'mipmap-xhdpi-v4': 96,
    'mipmap-xxhdpi-v4': 144,
    'mipmap-xxxhdpi-v4': 192
}

for folder, size in densities.items():
    dir_path = os.path.join('app/src/main/res', folder)
    os.makedirs(dir_path, exist_ok=True)
    
    # Square/squircle icon
    subprocess.run([
        'convert', 'icon_1024.png', '-resize', f'{size}x{size}',
        os.path.join(dir_path, 'ic_launcher.png')
    ], check=True)
    
    # Round icon
    subprocess.run([
        'convert', 'icon_round_1024.png', '-resize', f'{size}x{size}',
        os.path.join(dir_path, 'ic_launcher_round.png')
    ], check=True)

# Copy main 1024 icon to res/drawable/amar_hishab_icon.png
subprocess.run(['cp', 'icon_1024.png', 'app/src/main/res/drawable/amar_hishab_icon.png'], check=True)

# Remove old jpg
if os.path.exists('app/src/main/res/drawable/amar_hishab_icon.jpg'):
    os.remove('app/src/main/res/drawable/amar_hishab_icon.jpg')

print("All mipmap and drawable icon assets generated and placed!")
