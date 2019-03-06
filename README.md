FloodShield: Securing the SDN Infrastructure Against Denial-of-Service Attacks
====================================

A brief introduction
-------------

Software-Defined Networking (SDN) has attracted great attention from both academia and industry. However, the deployment of SDN has faced some critical security issues, such as Denial-of-Service (DoS) attacks on the SDN infrastructure. One such DoS attack is the data-to-control plane saturation
attack, where an attacker floods a large number of packets to trigger massive table-misses and packet-in messages in the data plane. This attack can exhaust resources of different components of the SDN infrastructure, including TCAM and
buffer memory in the data plane, bandwidth of the control
channel, and CPU cycles of the controller. 

In this paper, we analyze the vulnerability of SDN against the data-to-control plane saturation attack extensively and design FloodShield, a comprehensive, deployable and lightweight SDN defense framework to mitigate this dedicated DoS attack. FloodShield combines the following two techniques: 1) source address validation which filters forged packets directly in the data plane, and 2) stateful packet supervision which monitors traffic states of real addresses and performs dynamic countermeasures based on evaluation scores
and network resource usages. Implementations and experiments demonstrate that, in comparison with state-of-the-art defense frameworks, FloodShield provides effective protection for all three components of the SDN infrastructure – data plane, control channel and control plane – with less resource consumption. Please refer to our [paper](https://zhangmenghao.github.io/papers/trustcom2018-floodshield-securing.pdf) on more details.
