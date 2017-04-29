#!/bin/bash
# attack at different pps

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

function rand(){
    min=$1
    max=$(($2-$min+1))
    num=$(($RANDOM+1000000000))
    echo $(($num%$max+$min))
}

if [[ `lsmod | grep pktgen` == "" ]];then
    modprobe pktgen
fi

name=`ifconfig | head -1 | awk -F \t '{print $1}' | awk -F - '{print $1}'`
name=`ifconfig | head -1 | awk -F \t '{print $1}' | awk -F - '{print $1}'`
if [[ $1 == "of" ]];then
   ip='10.0.0.'${name:((${#var} - 1))}
else
   ip=`cat ip | grep $name | awk '{print $2}'`
fi
mac='00:00:00:00:00:0'${name:((${#var} - 1))}
device=`ifconfig | head -1 | awk '{OFS="\t"}{print $1}'`
ipHi=`./ipHi.py $ip`

i=1
ips='10.0.0.2'
ipd=''
while [ "$i" -le "10000" ]; do
    pps=`cat $2 | awk '{if (NR=='''$i''') print $1}'`
    num=`cat $2 | awk '{if (NR=='''$i''') print $2}'`
    ipd=`./ipPlus.py $ips $num`
    # echo "Adding devices to run".
    PGDEV=/proc/net/pktgen/kpktgend_0
    BASIC_DELAY=1000000000
    pgset "rem_device_all"
    pgset "add_device $device"
    pgset "max_before_softirq 10000"
    # Configure the individual devices
    PGDEV=/proc/net/pktgen/$device
    pgset "clone_skb 0"
    pgset "pkt_size 60"
    pgset "src_mac $mac"
    pgset "src_min $ip"
    pgset "src_max $ipHi"
    pgset "dst_min $ips"
    pgset "dst_max $ipd"
    pgset "dst_mac 00:00:00:00:00:01"
    pgset "dst_mac_count 8"
    PGDEV=/proc/net/pktgen/$device
    echo $ips" "$ipd" running at pps "$pps
    delay=`expr ${BASIC_DELAY} / $pps`
    pgset "count $num"
    pgset "delay $delay"
    PGDEV=/proc/net/pktgen/pgctrl
    pgset "start"
    i=$(($i+1))
    ips=$ipd
done
echo "end"