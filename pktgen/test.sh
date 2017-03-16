#!/bin/sh
# pktgen.conf -- Sample configuration for send on two devices on a UP system
#modprobe pktgen
function pgset(){
	local result
        echo $1 > $PGDEV   
        result=`cat $PGDEV | fgrep "Result: OK:"`
	#local content
	#content=`cat $PGDEV`	
	#echo $content      
	if [ "$result" = "" ]; then
             cat $PGDEV | fgrep Result:
        fi
}
    
function pg() {
    echo inject > $PGDEV
    cat $PGDEV
}
    
    # On UP systems only one thread exists -- so just add devices
    # We use eth1, eth2
    
    echo "Adding devices to run".
    
    PGDEV=/proc/net/pktgen/kpktgend_0

    BASIC_DELAY=1000000000 #1pps
	BASIC_SPEED=1    
	SEND_TOTAL_TIME=20
	BLOCK_TIME=10
	DATA=a.txt
    	
    pgset "rem_device_all"
    pgset "add_device h2-eth0"
    pgset "max_before_softirq 10000"
    
    # Configure the individual devices
    echo "Configuring devices"
    
    PGDEV=/proc/net/pktgen/h2-eth0
    
    pgset "clone_skb 0"
    pgset "pkt_size 60"
    pgset "src_mac 00:00:00:00:00:02"
    pgset "src_min 10.0.0.152"
    pgset "src_max 10.0.0.152"
    pgset "dst_min 10.0.0.2"
    pgset "dst_max 10.255.255.255"
    pgset "dst_mac 00:00:00:00:00:03"

    
    # Time to run
    
   
    SPEED=2
    echo "Running... ctrl^C to stop"
    for (( i=1; i<10000; i++ ));do
	PGDEV=/proc/net/pktgen/h2-eth0
	
	SPEED=`expr 1000 + $SPEED`
	DELAY=`expr ${BASIC_DELAY} / $SPEED`
	PPS=`expr ${SPEED} \* ${BASIC_SPEED}`
	if [ $PPS -gt 50000000 ];then
	    #echo $PPS
	    PPS=50000000
	fi
	PKT_COUNT=`expr $SEND_TOTAL_TIME \* $PPS`
	echo "${PPS}/pps,send ${PKT_COUNT}pkt"
	
	pgset "count $PKT_COUNT"
	pgset "delay $DELAY"	
	
	PGDEV=/proc/net/pktgen/pgctrl   
	pgset "start"
	
	CONTENT=`cat /proc/net/pktgen/h2-eth0`
	echo $CONTENT
	echo ${PPS} >> $DATA
	echo 'sleep ${BLOCK_TIME}s'
	#echo 'sleep' >> $DATA	
	sleep ${BLOCK_TIME}s
	echo 'wake up'
    done    
    echo "Done"
