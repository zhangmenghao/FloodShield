#!/bin/bash

function pgset() {
    local result
    echo $1 > $PGDEV
    result=`cat $PGDEV | fgrep "Result: OK:"`
    if [ "$result" = "" ]; then
        cat $PGDEV | fgrep Result:
    fi
}

function pg() {
    echo inject > $PGDEV
    cat $PGDEV
}

if [[ `lsmod | grep pktgen` == "" ]];then
    modprobe pktgen
fi

name=`ifconfig | head -1 | awk -F \t '{print $1}' | awk -F - '{print $1}'`
ip='10.0.0.'${name:((${#var} - 1))}
mac='00:00:00:00:00:0'${name:((${#var} - 1))}
device=`ifconfig | head -1 | awk '{OFS="\t"}{print $1}'`
pps=0
if [[ $1 == "0" ]];then
   echo 'exit'
   exit
else
   pps=$1
fi

echo "ip = $ip"
echo "mac = $mac"
echo "device = $device"
echo "pps = $pps"

# echo "Adding devices to run".
PGDEV=/proc/net/pktgen/kpktgend_0
BASIC_DELAY=1000000000
pgset "rem_device_all"
pgset "add_device $device"
pgset "max_before_softirq 10000"
# Configure the individual devices
echo "Configuring devices"
PGDEV=/proc/net/pktgen/$device

pgset "clone_skb 0"
pgset "pkt_size 60"
pgset "src_mac $mac"
pgset "src_min 10.0.0.1"
pgset "src_max 10.0.0.1"
pgset "dst_min 10.0.0.2"
pgset "dst_max 10.0.0.2"
pgset "udp_src_min 2"
pgset "udp_src_max 65535"
pgset "udp_dst_min 2"
pgset "udp_dst_max 65530"
pgset "dst_mac 00:00:00:00:00:02"

echo "Running... ctrl^C to stop"

PGDEV=/proc/net/pktgen/$device
delay=`expr ${BASIC_DELAY} / $pps`
pgset "count 0"
pgset "delay $delay"

PGDEV=/proc/net/pktgen/pgctrl
pgset "start"
