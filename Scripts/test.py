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

class MyTreeTopo( Topo ):
    "Topology for a tree network with a given depth and fanout."

    def build( self, depth=1, fanout=2 ):
        # Numbering:  h1..N, s1..M
        self.hostNum = 1
        self.switchNum = 1
        # Build topology
        self.addTree( depth, fanout )

    def addTree( self, depth, fanout ):
        """Add a subtree starting with node n.
           returns: last node added"""
        isSwitch = depth > 0
        if isSwitch:
            node = self.addSwitch( 's%s' % self.switchNum )
            self.switchNum += 1
            for _ in range( fanout ):
                child = self.addTree( depth - 1, fanout )
                self.addLink( node, child, bw=100, max_queue_size=1000)
        else:
            node = self.addHost( 'h%s' % self.hostNum )
            self.hostNum += 1
        return node

def simpleTest(ip):
    topo = MyTreeTopo(depth = 3, fanout = 2)
    net = Mininet(topo=topo, switch=OVSSwitch,
        controller=lambda name: RemoteController(name, ip=ip),
        autoSetMacs=True, link=TCLink)
    net.start()
    os.system('./limit.sh')
    CLI(net)
    net.stop()

if __name__ == '__main__':
    setLogLevel('info')
    simpleTest(sys.argv[1])
