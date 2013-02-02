Blending modes
==============

Most tools can have a *blending mode* specified for them. A blending
mode controls how the effects of tools in the [tool
stack](Tool_Stack.html) blend together to form the final photo. In order
to understand blending modes, there are a few terms to know:

Base color
  ~ This is the color of a pixel before a tool's effect has been applied
    to the photo.
Blend color
  ~ This is the color of a pixel after a tool's effect has been applied
    to the photo.
Result color
  ~ This is the color of a pixel after its base and blend colors have
    been blended together using a blending mode.

This is illustrated in the figure below. First, as shown by the green
arrows, the base colors of a photo have a tool's effect applied to them
producing the blend colors. Second, as shown by the red arrows, the base
and blend colors are blended together using a blending mode producing
the result colors.

![image](images/Blend_Modes_Illustration-en.png)

There are many blending modes to choose from. While descriptions for
each are below, it's best to experiment.

Normal
  ~ This blending mode has no effect; therefore, the blend and result
    colors are the same.

Average
  ~ This is the same as the Normal blending mode but at 50% opacity.

Color Burn
  ~ Darkens the base color to reflect the blend color by increasing
    contrast. (Opposite of Color Dodge. See also: Soft Burn.)

Color Dodge
  ~ Brightens the base color to reflect the blend color by decreasing
    contrast. (Opposite of Color Burn.)

Darken
  ~ The result color is either the base or blend color, whichever is
    darker. (Opposite of Lighten.)

Difference
  ~ Subtracts either the blend color from the base color or the base
    color from the blend color depending on whichever is brighter. (See
    also: Exclusion.)

Exclusion
  ~ Creates an effect similar to, but lower in contrast than, the
    Difference blending mode. (See also: Negation.)

Hard Light
  ~ This blending mode multiplies or screens the colors, depending on
    the blend color. If the blend color is lighter than 50% gray, the
    color is lightened (screened); if the blend color is darker than 50%
    gray, the color is darkened (multiplied). This is useful for adding
    highlights or shadows to an image.

Lighten
  ~ The result color is either the base or blend color, whichever is
    lighter. (Opposite of Darken.)

Midtones
  ~ This blending mode is the same as the Normal blending mode except
    that the effect is applied only to midtones.

Mid+Hilights
  ~ This blending mode is the same as the Normal blending mode except
    that the effect is applied only to midtones and highlights.

Multiply
  ~ This blending mode multiplies the base and blend colors. The result
    color is always darker. The effect is similar to projecting multiple
    photographic slides onto the same screen. (See also: Screen.)

Negation
  ~ This blending mode is similar to the Exclusion blending mode except
    that it shows brighter and more vibrant colors.

Overlay
  ~ Multiplies or screens the colors depending on the base color.
    Patterns or colors overlay existing pixels while preserving the
    highlights and shadows of the base color. The base color is not
    replaced, but is mixed with the blend color to reflect the relative
    lightness or darkness of the original color.

Screen
  ~ This blending mode is similar to the Multiply blending mode except
    that it multiplies the inverse of the blend and base colors. The
    result color is always lighter. The effect is similar to projecting
    multiple photographic slides onto the same screen.

Shadows
  ~ This blending mode causes only the shadows to be affected.

Soft Burn
  ~ This blending mode is similar to the Color Burn blending mode except
    that the effect results in less saturation and contrast.

Soft Dodge
  ~ This blending mode is a combination of both the Color Dodge and
    inverse Color Burn modes, but much smoother than either. The base
    colors are darkened slightly with very bright blend colors dodged
    in.

Soft Light
  ~ This blending mode darkens or lightens the colors depending on the
    blend color. The effect is similar to shining a diffused spotlight
    on the image. If the blend color is lighter, the image is lightened
    as if it were dodged; if the blend color is darker, the image is
    darkened as if it were burned in.

