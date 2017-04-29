import os
import re
import sys

h1h3 = {}
h1h7 = {}
h5h6 = {}

d = sys.argv[1] + '/'

for file in os.listdir(d):
    if 'rrt-' not in file:
        continue
    entrys = file.split('-')
    num = entrys[1]
    res = open(d + file).read()
    val = re.findall('rtt\smin/avg/max/mdev\s=\s([0-9\./]*)\sms', res)
    if len(val) > 0:
        val = val[0].split('/')[1]
    else:
        val = '999999'
    if 'h1-h3' in file:
        h1h3[num] = val
    if 'h1-h7' in file:
        h1h7[num] = val
    if 'h5-h6' in file:
        h5h6[num] = val

f = open(d + 'rrt.md', 'w')
f.write('| pps | h1-h3 | h5-h6 | h1-h7 |\n')
f.write('| :-----: | :-----: | :-----: | :-----: |\n')
arr = [' 100', ' 250', ' 500', ' 750', '1000', '1250', '1500', '1750', '2000']
for i in arr:
    f.write('| ' + i + ' |')
    f.write(' ' + h1h3[i.strip()] + ' |')
    f.write(' ' + h5h6[i.strip()] + ' |')
    f.write(' ' + h1h7[i.strip()] + ' |')
    f.write('\n')
f.close()