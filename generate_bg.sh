#!/bin/bash
W=1000
H=680

# 1. Base sky gradient: deep dark midnight blue to twilight cyan
convert -size ${W}x${H} -define gradient:direction=north gradient:'#030914'-'#061b33' base_sky.png

# 2. Add atmospheric moon glow in the sky at (780, 160)
convert -size ${W}x${H} xc:transparent \
  -fill "radial-gradient:#38bdf888-transparent" -draw "circle 780,160 780,380" \
  -fill "radial-gradient:#fef08a99-transparent" -draw "circle 780,160 780,260" \
  moon_glow.png

# 3. Moon body
convert -size ${W}x${H} xc:transparent \
  -fill '#fef9c3' -draw "circle 780,160 780,215" \
  moon_body.png

# 4. Composite sky + glow + moon
composite -compose Over moon_glow.png base_sky.png sky_stage1.png
composite -compose Over moon_body.png sky_stage1.png sky_stage2.png

# 5. Distant mountain layer (smooth rolling peaks)
convert -size ${W}x${H} xc:transparent -fill '#0a233d' -draw "
  path 'M 0,380 Q 200,280 400,340 T 700,250 T 880,280 L 1000,320 L 1000,680 L 0,680 Z'
" mountain_back.png

# 6. Mid mountain layer
convert -size ${W}x${H} xc:transparent -fill '#06172b' -draw "
  path 'M 0,420 Q 250,340 500,410 T 800,330 L 1000,370 L 1000,680 L 0,680 Z'
" mountain_mid.png

# 7. Lake surface (deep reflective dark water)
convert -size ${W}x${H} xc:transparent -fill '#041120' -draw "
  rectangle 0,460 1000,680
" lake.png

# 8. Forest & shoreline silhouette
convert -size ${W}x${H} xc:transparent -fill '#020b16' -draw "
  path 'M 0,470 Q 150,465 300,480 T 600,465 T 850,485 L 1000,475 L 1000,680 L 0,680 Z'
" shoreline.png

# 9. Tree silhouettes along shore
convert shoreline.png -fill '#020b16' \
  -draw "polygon 320,480 326,450 332,480" \
  -draw "polygon 335,480 342,442 349,480" \
  -draw "polygon 352,480 357,452 362,480" \
  -draw "polygon 560,468 566,438 572,468" \
  -draw "polygon 575,468 582,430 589,468" \
  -draw "polygon 592,468 598,442 604,468" \
  -draw "polygon 720,480 726,448 732,480" \
  -draw "polygon 735,480 742,438 749,480" \
  -draw "polygon 840,485 848,445 856,485" \
  trees.png

# 10. Water moonlight reflections (shimmer bars below moon at x=780, y=470..620)
convert -size ${W}x${H} xc:transparent \
  -stroke '#fde047aa' -strokewidth 2.5 \
  -draw "line 750,475 810,475" \
  -draw "line 730,490 830,490" \
  -draw "line 710,508 850,508" \
  -stroke '#fde04788' -strokewidth 3 \
  -draw "line 690,528 870,528" \
  -draw "line 670,550 890,550" \
  -stroke '#38bdf866' -strokewidth 2.5 \
  -draw "line 660,572 900,572" \
  -draw "line 680,595 880,595" \
  -draw "line 710,618 850,618" \
  water_reflection.png

# 11. Distant birds
convert -size ${W}x${H} xc:transparent -stroke '#94a3b8aa' -strokewidth 2 -fill transparent \
  -draw "path 'M 580,210 Q 588,202 596,210 Q 604,202 612,210'" \
  -draw "path 'M 605,230 Q 612,223 619,230 Q 626,223 633,230'" \
  -draw "path 'M 628,248 Q 634,242 640,248 Q 646,242 652,248'" \
  birds.png

# Composite all layers in order
convert sky_stage2.png mountain_back.png -composite step1.png
convert step1.png mountain_mid.png -composite step2.png
convert step2.png lake.png -composite step3.png
convert step3.png water_reflection.png -composite step4.png
convert step4.png trees.png -composite step5.png
convert step5.png birds.png -composite app/src/main/res/drawable/bg_balance_card_landscape.png

rm -f base_sky.png moon_glow.png moon_body.png sky_stage1.png sky_stage2.png mountain_back.png mountain_mid.png lake.png shoreline.png trees.png water_reflection.png birds.png step*.png
echo "Generated app/src/main/res/drawable/bg_balance_card_landscape.png"
ls -lh app/src/main/res/drawable/bg_balance_card_landscape.png
