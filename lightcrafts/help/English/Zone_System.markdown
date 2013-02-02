The Zone System
===============

The **Zone System** was developed by Ansel Adams and Fred Archer in the
1940s as a method for achieving consistent results from exposing
negative film all the way to paper prints. The Zone System is a
framework that photographers can use to think about their photos and
perform meaningful operations on them, such as altering exposure,
contrast, and most all other visual aspects of a photo.

With the [ZoneFinder](ZoneFinder.html) and
[ZoneMapper](Tool-ZoneMapper.html) tool, LightZone offers a unique
implementation of the Zone System that has been adapted from negative
film to digital media and interactive image processing allowed by modern
computers. If you are familiar with the Zone System, you probably have
noticed that there are differences between the traditional Zone System
and LightZone.

The Traditional Zone System

The traditional Zone System was developed for negative film and has 11
zones where each zone differs by one *exposure value* (one "EV" which is
equivalent to one f-stop). Middle (or 18%) gray falls onto zone 5.

The traditional Zone System takes into account the nonlinear nature of
both film and photographic paper that compresses the tonal values of
highlights and shadows (the "shoulder" and "toe" of photographic media).

LightZone's Digital Adaptation of the Zone System

LightZone instead was developed for digital cameras and computers and
has 16 zones where each zone differs by *half* an EV. Middle gray falls
onto the fifth zone from the top.

The reason for the differences are that digital cameras and computers
use a linear representation for values of light and that computer
displays have a dynamic range that is less than 8 EVs. Using half EV
spacing for zones in LightZone results in 16 zones and gives the best
compromise between screen space and precision.
