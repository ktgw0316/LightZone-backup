#!/bin/sh

# cf. https://www.owsiak.org/how-to-solve-missing-javah-ugly-way/

# FIRST_ARG - full class name (with package)
# SECOND_ARG - class path

CLASS_NAME=$(echo $1 | sed 's/.*\.//g')

PACKAGE_NAME=$(echo $1 | sed 's/\(.*\)\..*/\1/g')

DIR_NAME=$(echo $PACKAGE_NAME | sed 's|\.|/|g')
mkdir -p java_jni/${DIR_NAME}

JAVA_FILE_NAME="java_jni/${DIR_NAME}/${CLASS_NAME}.java"

echo "package ${PACKAGE_NAME};" > ${JAVA_FILE_NAME}
echo "public class ${CLASS_NAME} {" >> ${JAVA_FILE_NAME}

javap -p -cp $2 $1 | grep "native " | while read line; do
  line=$(echo $line | sed 's/java\.lang\.//g' \
      | sed 's/[^( )]\+\.[^,) ]\+/Object/g')
  args=$(echo $line | sed 's/.*(\(.*\)).*/\1/' | wc -w)
  if [ $args -gt 0 ]; then
    param=0
    comma=$(echo $line | grep -c "(.*,.*)")
    while [ $comma -gt 0 ]; do
      line=$(echo $line | sed "s/\((.*\),\(.*)\)/\1 param_${param}|\2/")
      param=$((param+1))
      comma=$(echo $line | grep -c "(.*,.*)")
    done
    line=$(echo $line | sed "s/)/ param_${param})/" | sed 's/|/,/g')
  fi
  line=$(echo $line | sed "s/ throws .*$/;/")
  echo "  $line" >> ${JAVA_FILE_NAME}
done

echo "}" >> ${JAVA_FILE_NAME}

mkdir -p javah
javac -h javah ${JAVA_FILE_NAME}

