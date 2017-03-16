import matplotlib.pyplot as plt
import sys
import getopt
import numpy as np

pps_data = list()
bw_data = list()


def read_pps_data(file):
    try:
        print file
        file_obj = open(file, "r")
        pps = file_obj.readline()
        while not pps == "":
            pps_data.append(pps)
            pps = file_obj.readline()
        return
    except Exception, e:
        print e
        return ""
    finally:
        file_obj.close()


def read_bw_data(file):
    try:
        file_obj = open(file, "r")
        bw_list = file_obj.readlines()[6:]
        for line in bw_list:
            bw = (line.split(" "))[-2]
            bw_data.append(bw)
        return

    except Exception, e:
        print e
        return ""
    finally:
        file_obj.close()


def show_help():
    pass

script_name = sys.argv[0]
print script_name
opts, values = getopt.getopt(sys.argv[1:], "h", ["pps=", "help", "bw="])
for opt, val in opts:
    if opt in ("-h", "--help"):
        show_help()
    if opt == "--pps":
        read_pps_data(val)
    if opt == "--bw":
        read_bw_data(val)
print pps_data
print bw_data
plt.plot(pps_data, bw_data, 'r')
plt.show()