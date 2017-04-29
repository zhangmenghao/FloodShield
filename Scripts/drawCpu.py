import sys
import matplotlib.pyplot as plt

def main(f1, f2):
    cpu1 = []
    cpu2 = []
    # old, f1
    r1 = open(f1).read().split('\n')
    rank = 0
    for line in r1:
        if len(line) == 0:
            continue
        cpu1.append(float(line))
    r2 = open(f2).read().split('\n')
    for line in r2:
        if len(line) == 0:
            continue
        cpu2.append(float(line))
    assert len(cpu1) == len(cpu2)
    x = range(0, len(cpu1))
    plt.figure(1)
    plt.subplot(111)
    plt.xlabel('time/sec')
    plt.ylabel('entry num')
    # plt.axis([0, 2100, 0, 10000])
    plt.plot(x, cpu1, 'b-', x, cpu2, 'r-')
    plt.show()


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])