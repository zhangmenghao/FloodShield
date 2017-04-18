from mininet.topo import *
from mininet.topolib import *
from mininet.link import TCLink
from mininet.net import Mininet
from mininet.node import Controller, RemoteController,OVSSwitch
from optparse import OptionParser
from mininet.log import setLogLevel
from mininet.cli import CLI

import os
import sys
import time

class MyTopo( Topo ):
    "Topology for a tree network with a given depth and fanout."

    def build(self):
        s1 = self.addSwitch('s1')
        h1 = self.addHost('h1')
        h2 = self.addHost('h2')
        self.addLink(s1, h1, bw=100, max_queue_size=1000)
        self.addLink(s1, h2, bw=100, max_queue_size=1000)

def simpleTest(ip, pps, way):
    topo = MyTopo()
    net = Mininet(topo=topo, switch=OVSSwitch,
        controller=lambda name: RemoteController(name, ip=ip),
        autoSetMacs=True, link=TCLink)
    net.start()
    hosts = net.hosts
    hosts[0].cmd('./' + way + '.sh ' + pps + ' &')
    pid = int(hosts[0].cmd('echo $!'))
    print 'h1 attacking', pid
    os.system('./limit0.sh')
    CLI(net)
    hosts[0].cmd('kill -9 ', pid)
    net.stop()

if __name__ == '__main__':
    setLogLevel('info')
    simpleTest(sys.argv[1], sys.argv[2], sys.argv[3])
