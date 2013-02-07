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

while read LINE; do
  echo '\n\\clearpage\n' >> ${UNIFIED}
  sed -f ${SEDFILE} ${LINE}.markdown >> ${UNIFIED}
done < ${LIST}

pandoc -f markdown --toc -o ${OUTPUT} ${UNIFIED}

rm ${UNIFIED}
cd -
