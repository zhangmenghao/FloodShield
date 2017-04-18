import sys

def main(f):
    items = open(f).read().split('\n')
    s1 = 0
    s2 = 0
    s3 = 0
    for item in items:
        if len(item) == 0:
            continue
        sws = item.split(' ')
        for sw in sws:
            name = sw[ : sw.find('-')]
            num = sw[sw.find('-') + 1 : ]
            if name == 's1':
                s1 += int(num)
            if name == 's2':
                s2 += int(num)
            if name == 's3':
                s3 += int(num)
        print 's1-' + str(s1) + ' s2-' + str(s2) + ' s3-' + str(s3)
if __name__ == '__main__':
    main(sys.argv[1])