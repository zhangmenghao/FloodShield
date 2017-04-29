flowLimit=3000
sudo ovs-vsctl -- --id=@s10 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s1 flow_tables:0=@s10
sudo ovs-vsctl -- --id=@s11 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s1 flow_tables:1=@s11
sudo ovs-vsctl -- --id=@s30 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s2 flow_tables:0=@s30
sudo ovs-vsctl -- --id=@s31 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s2 flow_tables:1=@s31
sudo ovs-vsctl -- --id=@s30 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s3 flow_tables:0=@s30
sudo ovs-vsctl -- --id=@s31 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s3 flow_tables:1=@s31
sudo ovs-vsctl -- --id=@s40 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s4 flow_tables:0=@s40
sudo ovs-vsctl -- --id=@s41 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s4 flow_tables:1=@s41
sudo ovs-vsctl -- --id=@s50 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s5 flow_tables:0=@s50
sudo ovs-vsctl -- --id=@s51 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s5 flow_tables:1=@s51
sudo ovs-vsctl -- --id=@s60 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s6 flow_tables:0=@s60
sudo ovs-vsctl -- --id=@s61 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s6 flow_tables:1=@s61
sudo ovs-vsctl -- --id=@s70 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s7 flow_tables:0=@s70
sudo ovs-vsctl -- --id=@s71 create Flow_Table flow_limit=$flowLimit overflow_policy=evict -- set Bridge s7 flow_tables:1=@s71