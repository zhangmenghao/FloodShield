#!/bin/bash

function rand(){
    min=$1
    max=$(($2-$min+1))
    num=$(($RANDOM+1000000000))
    echo $(($num%$max+$min))
}

i=1
while [ "$i" -le "10000" ]; do
    pps=$(rand 100 1000)
    pp=$((pps*2))
    delta=$(rand 50 $pp)
    num=$(($delta+$pps*2))
    echo $pps" "$num
    i=$(($i+1))
done