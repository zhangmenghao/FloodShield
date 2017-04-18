import socket

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.connect(('8.8.8.8', 0))
local_ip_address = s.getsockname()[0]
print local_ip_address