#!/bin/sh
java -Xmx256m -jar rtr.jar test tester p4lang- other p4lang2.ini summary slot 123 url http://sources.nop.hu/cfg/ $1 $2 $3 $4 $5 $6 $7 $8
