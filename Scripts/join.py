import sys

def main(s1, s2, s3):
    s1 = open(s1).read().split('\n')
    s2 = open(s2).read().split('\n')
    s3 = open(s3).read().split('\n')
    leng = min(len(s1), len(s2))
    leng = min(len(s3), leng)
    leng -= 1
    for i in range(0, leng):
        print 's1-' + s1[i] + ' s2-' + s2[i] + ' s3-' + s3[i]


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3])