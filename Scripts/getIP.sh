name=`ifconfig | head -1 | awk -F \t '{print $1}' | awk -F - '{print $1}'`
echo $name
if [ $name = "h1" ]; then
    > ip
fi
ip=`python getIP.py`
echo $name" "$ip >> ip