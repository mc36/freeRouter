description openvpn over openvpn

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
!
vrf def v1
 rd 1:1
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 exit
crypto ipsec ips
 cipher des
 hash md5
 key $v10$MjJmOWM2NzZmNjU1MzM2YzNmMzE4OGI4ZDljYzc1OTkwMzczMzIxMmVkNzcyMzFiYzM4MTI2YjYwMDBiMDQzZjFmNTZkMDdiODg1ZjRkMDA2NzZhZmQ4ZmVhMjVjODhmYTkxNzI5NGQ4ZjFlODliODQ5MjJkNWQyNTU2ZGU5NzdiZWFjMmYyNTRiYTJiNjc0NzcxMzFmNGQ0NzA4Y2I1MDlmNGM5Zjc4NDc4MDQ2NTQ2MmU1MDJkMjkxODM2NjViYmQ1ZWZmNmJkYzI3MzcwZjA1YWExZDg1NmI0OTdhMWY3ZWY1ZjIwYmFkN2FmZjE1NTYxOWE0YjA5ODQ5ZmFiODE0ZWU3NmU3MTIxYzJhZGY4NTMyNmRiNGMxY2NlMTMyMjAwY2EzZTRkMDM5MzBmNzY1YmE5NmE4YzQ2ZjFhYjM3NGJlYjczZTc5MDkzZDYwODc5YThkOTU4NWYyZmViOTg3ZDg5ZTY1YTMzZWYzODU3ZjNiMDlkZjgwYTI0MDNmNmM1MGRjNTA0MzllMjU4ZDYxYzdkYWMzNzc1MTRhOGQyODFjMTBmZWVlYTc5YWU3YjA2MzA2NGFlYzM5ODliNGQ4NjdiYjI0MTgyZjdkMDA3YWQ0MTI4NGVlNjU3NzA1M2RhZTJjYzI4OWRkMzllNjZjZDhmZTcwODliNzAxNWY=
 exit
int tun2
 tunnel vrf v1
 tunnel prot ips
 tunnel mode openvpn
 tunnel source ethernet1
 tunnel destination 1.1.1.2
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.0
 exit
int tun1
 tunnel vrf v1
 tunnel prot ips
 tunnel mode openvpn
 tunnel source tun2
 tunnel destination 2.2.2.2
 vrf for v1
 ipv4 addr 3.3.3.1 255.255.255.0
 ipv6 addr 1234::1 ffff::
 exit
!

addrouter r2
int eth1 eth 0000.0000.2222 $1b$ $1a$
!
vrf def v1
 rd 1:1
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.2 255.255.255.0
 exit
crypto ipsec ips
 cipher des
 hash md5
 key $v10$MjJmOWM2NzZmNjU1MzM2YzNmMzE4OGI4ZDljYzc1OTkwMzczMzIxMmVkNzcyMzFiYzM4MTI2YjYwMDBiMDQzZjFmNTZkMDdiODg1ZjRkMDA2NzZhZmQ4ZmVhMjVjODhmYTkxNzI5NGQ4ZjFlODliODQ5MjJkNWQyNTU2ZGU5NzdiZWFjMmYyNTRiYTJiNjc0NzcxMzFmNGQ0NzA4Y2I1MDlmNGM5Zjc4NDc4MDQ2NTQ2MmU1MDJkMjkxODM2NjViYmQ1ZWZmNmJkYzI3MzcwZjA1YWExZDg1NmI0OTdhMWY3ZWY1ZjIwYmFkN2FmZjE1NTYxOWE0YjA5ODQ5ZmFiODE0ZWU3NmU3MTIxYzJhZGY4NTMyNmRiNGMxY2NlMTMyMjAwY2EzZTRkMDM5MzBmNzY1YmE5NmE4YzQ2ZjFhYjM3NGJlYjczZTc5MDkzZDYwODc5YThkOTU4NWYyZmViOTg3ZDg5ZTY1YTMzZWYzODU3ZjNiMDlkZjgwYTI0MDNmNmM1MGRjNTA0MzllMjU4ZDYxYzdkYWMzNzc1MTRhOGQyODFjMTBmZWVlYTc5YWU3YjA2MzA2NGFlYzM5ODliNGQ4NjdiYjI0MTgyZjdkMDA3YWQ0MTI4NGVlNjU3NzA1M2RhZTJjYzI4OWRkMzllNjZjZDhmZTcwODliNzAxNWY=
 exit
int tun2
 tunnel vrf v1
 tunnel prot ips
 tunnel mode openvpn
 tunnel source ethernet1
 tunnel destination 1.1.1.1
 vrf for v1
 ipv4 addr 2.2.2.2 255.255.255.0
 exit
int tun1
 tunnel vrf v1
 tunnel prot ips
 tunnel mode openvpn
 tunnel source tun2
 tunnel destination 2.2.2.1
 vrf for v1
 ipv4 addr 3.3.3.2 255.255.255.0
 ipv6 addr 1234::2 ffff::
 exit
!

r1 tping 100 5 3.3.3.2 /vrf v1
r2 tping 100 5 3.3.3.1 /vrf v1
r1 tping 100 5 1234::2 /vrf v1
r2 tping 100 5 1234::1 /vrf v1
