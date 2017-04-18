#!/bin/sh

lo=`head -1 $1`
hi=`tail -1 $1`
n0=`date -d "$lo" +%s`
n1=`date -d "$hi" +%s`
i=$n0
while [ "$i" -le "$n1" ]; do
    t=`date -d @"$i"  "+%Y-%m-%d %H:%M:%S"`
    num=`cat $1 | grep "$t" | awk 'END{print NR}'`
    echo $num
    i=$(($i+1))
done