#!/bin/sh
DIR0=`dirname $0`
DIR=`cd ${DIR0}; pwd`
SEDFILE=${DIR}/html2unified.sed
LIST=_TOC.list
UNIFIED=UNIFIED.markdown
OUTPUT=LightZone-help.pdf

cd $1

if [ -e ${UNIFIED} ]; then 
  rm ${UNIFIED}
fi

cp ../neutral/images/* images/

while read LINE; do
  echo '\n\\clearpage\n' >> ${UNIFIED}

  pandoc -f html -t markdown --atx-headers ${LINE}.html > tmp.markdown 
  sed -n -e '/^#/,$p' tmp.markdown > ${LINE}.markdown
  sed -f ${SEDFILE} ${LINE}.markdown >> ${UNIFIED}
done < ${LIST}

pandoc -f markdown --toc --toc-depth=2 --latex-engine=xelatex -o ${OUTPUT} ${UNIFIED}

# Remove temporary files
for i in ../neutral/images/*; do
  rm images/`basename $i`
done
rm ${UNIFIED}
rm tmp.markdown

cd -
