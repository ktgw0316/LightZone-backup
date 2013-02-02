Black point compensation
========================

When either [converting](Converting.html) or [printing](Printing.html)
your photos using application managed colors, you can enable *black
point compensation* when the [rendering intent](Rendering_Intent.html)
is Relative Colorimetric.

The RGB color space can represent all luminosities from 100% white to
100% black. Output devices (such as printers and computer displays)
often have limitations in how black the darkest black they can render
is. Black point compensation attempts to resolve the difference between
the darkest black in your photo and the darkest black that a particular
device can render.

![image](images/Black_Point_Compensation-en.png) If no black point
compensation is done, then detail will be lost in the darkest shadow
areas of your photo because the dark areas beyond the darkest black the
device can render are truncated.

On the other hand, if black point compensation is done, the luminosity
range of your photo is compressed to fit into the luminosity range of
the device. Hence, the darkest black of your photo becomes the darkest
black that the device can render.

However, because the luminosity range is compressed, information from
your photo is still lost: the loss is just evenly spread out over the
entire luminosity range (and thus less noticeable) rather than being
truncated at the black end.
