set a [exec "show ipv4 bgp 1955 multicast allroute 195.111.0.0/16 | inc preference"]
puts "$a"
