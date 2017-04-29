import urllib2, cookielib
import json
import time

def getSwOF(pid, type):
    url = 'http://127.0.0.1:8080/wm/core/switch/' + pid + '/' + type + '/json'
    cookies = urllib2.HTTPCookieProcessor()
    opener = urllib2.build_opener(cookies)
    headers = {"Content-Type": "application/json"}
    request = urllib2.Request(url=url, headers=headers)
    response = opener.open(request)
    res = response.read()
    res = json.loads(res)
    if type == 'port':
        reply = res['port_reply'][0]['port']
        print reply
        for entry in reply:
            num = entry['port_number']
            recePk = int(entry['receive_packets'])
            sendPk = entry['transmit_packets']
            print num, recePk
    elif type == 'aggregate':
        print res
        num = int(res['aggregate']['packet_count'])
        print num
    else:
        print res

def getOF(url):
    cookies = urllib2.HTTPCookieProcessor()
    opener = urllib2.build_opener(cookies)
    headers = {"Content-Type": "application/json"}
    request = urllib2.Request(url=url, headers=headers)
    response = opener.open(request)
    res = response.read()
    res = json.loads(res)
    print res

getSwOF('00:00:00:00:00:00:00:03', 'port')

# getOF('http://127.0.0.1:8080/wm/core/switch/all/role/json')