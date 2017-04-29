#!/bin/sh
i=1
while [ "$i" -le "600" ]; do
    s1=`ovs-ofctl dump-flows s1 | grep cookie | awk 'END{print NR}'`
    s2=`ovs-ofctl dump-flows s2 | grep cookie | awk 'END{print NR}'`
    s3=`ovs-ofctl dump-flows s4 | grep cookie | awk 'END{print NR}'`
    echo "s3-"$s3" s2-"$s2" s1-"$s1
    sleep 1s
    i=$(($i+1))
done
echo 'end'