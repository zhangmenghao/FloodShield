i=1

while [ "$i" -le "10" ]; do
    pps=`cat $1 | awk '{if (NR=='''$i''') print $1}'`
    num=`cat $1 | awk '{if (NR=='''$i''') print $2}'`
    echo $pps"-"$num
    i=$(($i+1))
done
echo "end"