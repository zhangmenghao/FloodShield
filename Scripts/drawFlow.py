import sys
import matplotlib.pyplot as plt

def main(f1, f2):
    s1 = []
    s2 = []
    s3 = []
    expr1 = []
    expr2 = []
    expr3 = []
    # old, f1
    r1 = open(f1).read().split('\n')
    rank = 0
    for line in r1:
        rank += 1
        if len(line) == 0:
            continue
        nums = line.split(' ')
        assert len(nums) == 3
        for num in nums:
            vals = num.split('-')
            if vals[0] == 's1':
                s1.append(int(vals[1]))
            elif vals[0] == 's2':
                s2.append(int(vals[1]))
            elif vals[0] == 's3':
                s3.append(int(vals[1]))
    r2 = open(f2).read().split('\n')
    for line in r2:
        if len(line) == 0:
            continue
        nums = line.split(' ')
        assert len(nums) == 3
        for num in nums:
            vals = num.split('-')
            if vals[0] == 's1':
                expr1.append(int(vals[1]))
            elif vals[0] == 's2':
                expr2.append(int(vals[1]))
            elif vals[0] == 's3':
                expr3.append(int(vals[1]))
    assert len(expr1) == len(expr2)
    assert len(expr1) == len(expr3)
    x = range(0, len(s1))
    plt.figure(1)
    plt.subplot(111)
    plt.xlabel('time/sec')
    plt.ylabel('entry num')
    # plt.axis([0, 2100, 0, 10000])
    plt.plot(x, s1, 'b-', x, s2, 'r-', x, s3, 'g-', x, expr1, 'b--', expr2, 'r--', expr3, 'g--')
    plt.show()


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])