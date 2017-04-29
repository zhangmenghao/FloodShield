import sys
import matplotlib.pyplot as plt

def main(f1, f2):
    arr1 = []
    arr2 = []
    # old, f1
    r1 = open(f1).read().split('\n')
    rank = 0
    for line in r1:
        if len(line) == 0:
            continue
        arr1.append(float(line))
    r2 = open(f2).read().split('\n')
    for line in r2:
        if len(line) == 0:
            continue
        arr2.append(float(line))
    assert len(arr1) == len(arr2)
    x = range(0, len(arr1))
    plt.figure(1)
    plt.subplot(111)
    plt.xlabel('x')
    plt.ylabel('y')
    # plt.axis([0, 2100, 0, 10000])
    plt.plot(x, arr1, 'b-', x, arr2, 'r-')
    plt.show()


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])