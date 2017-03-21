import urllib2, cookielib
import matplotlib.pyplot as plt
import json
import time


pps_list = list()
total_invalid_rate_list = list()
total_flow_number_list = list()
pps_file = "a.txt"
sleep_time = 2
ave_time = 5
retrive_time = 5


def get_invalid_rate_and_fnmb():
    cookies = urllib2.HTTPCookieProcessor()
    opener = urllib2.build_opener(cookies)
    url = "http://127.0.0.1:8080/wm/statistics/flow/statistics/json"
    headers = {"Content-Type": "application/json"}
    request = urllib2.Request(url=url, headers=headers)

    response = opener.open(request)
    res = response.read()
    res = json.loads(res)
    rate = res["INVALID_RATE"]
    nmb = res["FLOW_NUMBER"]
    return rate, nmb


def get_pps(file):
    try:
        print file
        file_obj = open(file, "r")
        pps = file_obj.readlines()[-1]
        return pps
    except Exception, e:
        print e
        return ""
    finally:
        file_obj.close()

# pps change time
for i in range(retrive_time):
    rate = 0.0
    fnmb = 0.0

    pps = get_pps(pps_file)
    while pps == "" or pps == "sleep" or pps == "sleep\n":
        time.sleep(1)
        print pps + "reread"
        pps = get_pps(pps_file)
    # every time fetch ave_time
    for j in range(ave_time):
        rate_j, fnmb_j = get_invalid_rate_and_fnmb()
        rate += rate_j
        fnmb += fnmb_j
        time.sleep(sleep_time)
        latest_pps = get_pps(pps_file)
        if not latest_pps == pps:
            break

    rate = rate / (j+1)
    fnmb = fnmb / (j+1)

    pps_list.append(pps)
    total_flow_number_list.append(fnmb)
    total_invalid_rate_list.append(rate)

    time.sleep(sleep_time)

count = 1
rate = total_invalid_rate_list[0]
flow_nmb = total_flow_number_list[0]

pps_unique_list = list()
invalid_unique_rate = list()
total_flow_nmb_unique_list = list()
pps_unique_list.append(pps_list[0])


for i in range(1, len(pps_list)):
    old = pps_list[i-1]
    if pps_list[i] == old:
        count = count + 1
        rate += total_invalid_rate_list[i]
        flow_nmb += total_flow_number_list[i]
    else:
        pps_unique_list.append(pps_list[i])
        invalid_unique_rate.append(float(rate) / float (count))
        total_flow_nmb_unique_list.append(float(flow_nmb) / float (count))

        count = 1
        rate = total_invalid_rate_list[i]
        flow_nmb = total_flow_number_list[i]

lens = len(pps_unique_list)
if len(invalid_unique_rate) < lens:
    lens = len(invalid_unique_rate)

pps_unique_list = pps_unique_list[0:lens]
invalid_unique_rate = invalid_unique_rate[0:lens]
total_flow_nmb_unique_list = total_flow_nmb_unique_list[0:lens]

print total_flow_nmb_unique_list
print pps_list
print pps_unique_list
print invalid_unique_rate

plt.plot(pps_unique_list, total_flow_nmb_unique_list, 'g')
plt.show()
