#!/bin/sh
echo signing
java -Xmx256m -jar rtr.jar show version number | head -n 1 > rtr.txt
echo -n "version="
cat rtr.txt
java -Xmx256m -jar rtr.jar test verfile http://keystore.local/freertr2.key
java -Xmx256m -jar rtr.jar test vercore http://keystore.local/freertr2.key http://keystore.local/freertr2.key
echo verifying
java -Xmx256m -jar rtr.jar exec flash verify
