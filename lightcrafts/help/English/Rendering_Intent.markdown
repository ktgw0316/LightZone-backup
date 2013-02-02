Rendering intent
================

When either [converting](Converting.html) or [printing](Printing.html)
your photos using application managed colors, you can select the
*rendering intent*. The rending intents you can select are described
below.

Absolute Colorimetric
  ~ Leaves colors that fall inside the destination gamut unchanged.
    Out-of-gamut colors are clipped. No scaling of colors to the
    destination white point is performed. This intent tries to maintain
    color accuracy at the expense of preserving relationships between
    colors and is suitable for proofing to simulate the output of a
    particular device. This intent is particularly useful for previewing
    how paper color affects printed colors.
Relative Colorimetric
  ~ Compares the extreme highlight of the source color space to that of
    the destination color space and shifts all colors accordingly.
    Out-of-gamut colors are shifted to the closest reproducible color in
    the destination color space. This preserves more of the original
    colors in a photo than Perceptual. (See also [Black point
    compensation](Black_Point_Compensation.html).)
Perceptual
  ~ Tries to preserve the visual relationship between colors so it is
    perceived as natural to the human eye even though the color values
    themselves may change. This intent is suitable for photos with lots
    of out-of-gamut colors.
Saturation
  ~ Tries to produce vivid colors in a photo at the expense of color
    accuracy. This intent is suitable for business graphics like graphs
    or charts where bright saturated colors are more important than the
    exact relationship between colors.

