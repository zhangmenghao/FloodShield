#!/bin/sh
name=`ifconfig | head -1 | awk '{OF="\t"}{print $1}'`
if [ $name = "h1-eth0" ]; then
    killall dhcpd
fi