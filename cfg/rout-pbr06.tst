description policy routing with nexthop on interface between vrfs

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
!
vrf def v1
 rd 1:1
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.101 255.255.255.255
 ipv6 addr 4321::101 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.252
 ipv6 addr 1234:1::1 ffff:ffff::
 exit
access-list a2b4
 permit all 2.2.2.101 255.255.255.255 all 2.2.2.201 255.255.255.255 all
 exit
access-list a2b6
 permit all 4321::101 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff all 4321::201 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff all
 exit
ipv4 pbr v1 a2b4 v1 next 1.1.1.2
ipv6 pbr v1 a2b6 v1 next 1234:1::2
!

addrouter r2
int eth1 eth 0000.0000.2222 $1b$ $1a$
int eth2 eth 0000.0000.2222 $2a$ $2b$
!
vrf def v1
 rd 1:1
 exit
vrf def v2
 rd 1:1
 exit
access-list a2b4
 permit all 2.2.2.101 255.255.255.255 all 2.2.2.201 255.255.255.255 all
 exit
access-list b2a4
 permit all 2.2.2.201 255.255.255.255 all 2.2.2.101 255.255.255.255 all
 exit
access-list a2b6
 permit all 4321::101 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff all 4321::201 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff all
 exit
access-list b2a6
 permit all 4321::201 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff all 4321::101 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff all
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.2 255.255.255.252
 ipv6 addr 1234:1::2 ffff:ffff::
 ipv4 pbr a2b4 v2 next 1.1.1.5
 ipv6 pbr a2b6 v2 next 1234:2::1
 exit
int eth2
 vrf for v2
 ipv4 addr 1.1.1.6 255.255.255.252
 ipv6 addr 1234:2::2 ffff:ffff::
 ipv4 pbr b2a4 v1 next 1.1.1.1
 ipv6 pbr b2a6 v1 next 1234:1::1
 exit
!

addrouter r3
int eth1 eth 0000.0000.3333 $2b$ $2a$
!
vrf def v1
 rd 1:1
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.201 255.255.255.255
 ipv6 addr 4321::201 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.5 255.255.255.252
 ipv6 addr 1234:2::1 ffff:ffff::
 exit
access-list b2a4
 permit all 2.2.2.201 255.255.255.255 all 2.2.2.101 255.255.255.255 all
 exit
access-list b2a6
 permit all 4321::201 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff all 4321::101 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff all
 exit
ipv4 pbr v1 b2a4 v1 next 1.1.1.6
ipv6 pbr v1 b2a6 v1 next 1234:2::2
!


r1 tping 100 5 2.2.2.201 /vrf v1 /int lo0
r1 tping 100 5 4321::201 /vrf v1 /int lo0
r3 tping 100 5 2.2.2.101 /vrf v1 /int lo0
r3 tping 100 5 4321::101 /vrf v1 /int lo0

r1 tping 0 5 2.2.2.201 /vrf v1
r1 tping 0 5 4321::201 /vrf v1
r3 tping 0 5 2.2.2.101 /vrf v1
r3 tping 0 5 4321::101 /vrf v1

r2 output show ipv4 pbr v1
r2 output show ipv6 pbr v1
output ../binTmp/rout-pbr.html
<html><body bgcolor="#000000" text="#FFFFFF" link="#00FFFF" vlink="#00FFFF" alink="#00FFFF">
here are the ipv4 forwarding:
<pre>
<!>show:0
</pre>
here are the ipv6 forwarding:
<pre>
<!>show:1
</pre>
</body></html>
!
