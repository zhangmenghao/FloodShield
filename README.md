Flow-Watcher: A DoS Attack Defense Extension for the Infrastructure of Software-Defined Networks
====================================

A brief introduction
-------------------

[![Build Status](https://travis-ci.org/floodlight/floodlight.svg?branch=master)](https://travis-ci.org/floodlight/floodlight)

A brief introduction
---------------

By decoupling the control plane from the data plane, Software-Defined Networking(SDN) provides unprecedented programmability, automation and network innovations to the traditional network.
As a representation technique of SDN, [OpenFlow](https://www.opennetworking.org/en/sdn-resources/openflow) introduces a reactive mechanism for packet processing, which enables SDN to adapt to network dynamcis quickly. However, this mechanism may also become a new vulnerability of SDN infrastructure.
An attacker can exploit the vulnerability of SDN by controlling a number of zombie hosts under SDN and generating a large nubmer of malicious packets.

To mitigate this DoS attack, we proposes Flow-Watcher, a deployable and comprehensive SDN defense framework, combining two techniques, **source address validation** and **packet-in rate supervision**.
Through concrete code implementation and detailed experiment test, we demonstrate flow-Watcher provides better scalability, robustness and security for SDN/OpenFlow with only minor overhead.

Contact us
---------------------
Any suggestion is appreciated as FTGuard is still a research prototype. Please feel free to fork FTGuard from us on the github or send email(zhangmenghao0503@gmail.com) to us for any questions.
