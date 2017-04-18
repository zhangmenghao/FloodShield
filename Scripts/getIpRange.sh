#!/bin/sh

cat ip | awk -F . '{print $NF}' > temp
min=9999
cat temp | while read line
do
    line=$(( line ))
    if [ $line -lt $min ]
    then
        echo $line"<"$min
        min=$line
    fi
done
echo $min