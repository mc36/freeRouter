#!/bin/sh
#sudo apt install bf_sde psmisc iproute2 net-tools socat tshark iperf gcc git telnet
#$SDE/p4studio_build/p4studio_build.py
#rm -rf $SDE/build
#rm -rf $SDE/p4studio_build/p4studio_logs
#rm -rf $SDE/p4studio_build/third_party
#rm -rf $SDE/packages
#rm -f `find $SDE -name *.a`
#
#git clone ssh://git@bitbucket.software.geant.org:7999/rare/rare.git
#git clone ssh://git@bitbucket.software.geant.org:7999/rare/rare-bf2556x-1t.git
#gcc -O3 -o cons.bin cons.c
#cp initd /etc/init.d/rtr
#chmod 755 /etc/init.d/rtr
#update-rc.d rtr defaults
#systemctl mask serial-getty@ttyS0
#systemctl disable serial-getty@ttyS0
#echo "mc36 ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers
#
#fdisk /dev/sdb / p
#fsck -f /dev/sdb1
#resize2fs /dev/sdb1 6G
#fsck -f /dev/sdb1
#cfdisk /dev/sdb / resize 6.1G
#qemu-img resize --shrink p4bf.img 6.2G
#fallocate -d p4bf.img
#
cd /home/mc36/rare/p4src
export SDE=/home/mc36/bf-sde-9.4.0
export SDE_INSTALL=$SDE/install
#$SDE/install/bin/bf-p4c -I. $@ bf_router.p4
sudo -E $SDE/tools/p4_build.sh -I. $@ ./bf_router.p4
cd $SDE/logs/p4-build/bf_router
sudo csplit make.log /p4c/ /p4c/
tail -n+2 xx01
sudo rm xx0*
