#!/bin/sh
cd $1
for i in *.markdown; do
  NAME=`basename $i .markdown`
  pandoc -f markdown -t html --css="Help.css" --css="Platform.css" -B _BEFORE.html $i > ${NAME}.html
done
cd -
