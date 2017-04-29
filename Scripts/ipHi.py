#!/usr/bin/python

import sys

ip = sys.argv[1]
nums = ip.split('.')
s = int(nums[3]) + int(nums[2])*256 + int(nums[1])*65536
s += 1
# 10.s.s2.s3
s3 = s % 256
s /= 256
s2 = s % 256
s /= 256
print '10.' + str(s) + '.' + str(s2) + '.' + str(s3)