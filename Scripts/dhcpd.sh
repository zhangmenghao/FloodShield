#!/bin/sh
name=`ifconfig | head -1 | awk '{OF="\t"}{print $1}'`
if [ $name = "h1-eth0" ]; then
    sleep 5s
    chmod 777 /var/lib/dhcp/dhcpd.leases
    dhcpd
fi
ifconfig $name 0
dhclient $name

killall dhclient
num=`ps ax | grep "dhclient $name" | awk 'END{print NR}'`