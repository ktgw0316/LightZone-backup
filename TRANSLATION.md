# Translation How-to
This document describes how to use flatten/unflatten tools when you translate *.properties files.

To concatenate all property files into single file:

    cd lightcrafts
    ./tools/bin/lc-locale-flatten.sh > /tmp/source.properties
    cd ..

Then translate the source.properties into target.properties. I recommend to use a translation memory software such as OmegaT.

After the translation completed, convert UTF-8 chars to ISO-8859-1 and redivide the file:

    cd linux
    native2ascii /tmp/target.properties | ./tools/bin/lc-locale-unflatten.sh (locale)
    cd ..

Then rebuild the source:

    ant -f linux/build.xml

__note:__ Do not translate `*_Icon=...` lines, otherwise you will encounter `java.lang.illegalargumentexception input == null at javax.imageio.imageio.read` error when you starts LightZone.

