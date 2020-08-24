#ifndef _INGRESS_PARSER_P4_
#define _INGRESS_PARSER_P4_

/*------------------ I N G R E S S   P A R S E R -----------------------------*/

parser ig_prs_main(packet_in pkt,
                   /* User */
                   out headers hdr,
                   inout ingress_metadata_t ig_md,
                   /* Intrinsic */
                   inout standard_metadata_t ig_intr_md) {

    state start {
        transition select(ig_intr_md.ingress_port) {
CPU_PORT:
            prs_cpu;
        default:
            prs_data;
        }
    }

    state prs_data {
        ig_md.ingress_id = (SubIntId_t)ig_intr_md.ingress_port;
        transition select(ig_intr_md.instance_type) {
32w0:
            prs_ethernet;
        default:
            prs_cpu;
        }
    }

    state prs_cpu {
        pkt.extract(hdr.cpu);
        ig_md.ingress_id = hdr.cpu.port;
        transition prs_ethernet;
    }

    state prs_ethernet {
        pkt.extract(hdr.ethernet);
        transition select(hdr.ethernet.ethertype) {
0 &&& 0xfe00:
            prs_llc; /* LLC SAP frame */
0 &&& 0xfa00:
            prs_llc; /* LLC SAP frame */
ETHERTYPE_VLAN :
            prs_vlan;
ETHERTYPE_PPPOE_CTRL :
            prs_pppoeCtrl;
ETHERTYPE_PPPOE_DATA :
            prs_pppoeData;
ETHERTYPE_MPLS_UCAST :
            prs_mpls0;
ETHERTYPE_IPV4:
            prs_ipv4;
ETHERTYPE_IPV6:
            prs_ipv6;
ETHERTYPE_ARP:
            prs_arp;
ETHERTYPE_LACP:
            prs_control;
ETHERTYPE_LLDP:
            prs_control;
        default:
            accept;
        }
    }

    state prs_vlan {
        pkt.extract(hdr.vlan);
        transition select(hdr.vlan.ethertype) {
0 &&& 0xfe00:
            prs_llc; /* LLC SAP frame */
0 &&& 0xfa00:
            prs_llc; /* LLC SAP frame */
ETHERTYPE_PPPOE_CTRL :
            prs_pppoeCtrl;
ETHERTYPE_PPPOE_DATA :
            prs_pppoeData;
ETHERTYPE_MPLS_UCAST :
            prs_mpls0;
ETHERTYPE_IPV4:
            prs_ipv4;
ETHERTYPE_IPV6:
            prs_ipv6;
ETHERTYPE_ARP:
            prs_arp;
ETHERTYPE_LACP:
            prs_control;
ETHERTYPE_LLDP:
            prs_control;
        default:
            accept;
        }
    }

    state prs_pppoeCtrl {
        pkt.extract(hdr.pppoeC);
        ig_md.pppoe_ctrl_valid = 1;
        transition accept;
    }

    state prs_pppoeData {
        pkt.extract(hdr.pppoeD);
        ig_md.pppoe_data_valid = 1;
        transition select(hdr.pppoeD.ppptyp) {
0x0021:
            prs_ipv4;
0x0057:
            prs_ipv6;
0x0281:
            prs_mpls0;
        default:
            prs_pppoeDataCtrl;
        }
    }

    state prs_pppoeDataCtrl {
        ig_md.pppoe_ctrl_valid = 1;
        transition accept;
    }

    state prs_mpls0 {
        pkt.extract(hdr.mpls0);
        ig_md.mpls0_valid = 1;
        transition select(hdr.mpls0.bos) {
1w0:
            prs_mpls1;
1w1:
            prs_mpls_bos;
        default:
            accept;
        }
    }

    state prs_mpls1 {
        pkt.extract(hdr.mpls1);
        ig_md.mpls1_valid = 1;
        transition select(hdr.mpls1.bos) {
1w0:
            accept;
1w1:
            prs_mpls_bos;
        default:
            accept;
        }
    }

    state prs_mpls_bos {
        transition select((pkt.lookahead<bit<4>>())[3:0]) {
4w0x4:
            prs_ipv4; /* IPv4 only for now */
4w0x6:
            prs_ipv6; /* IPv6 is in next lab */
        default:
            prs_eth2; /* EoMPLS is pausing problem if we don't resubmit() */
        }
    }

    state prs_eth2 {
        pkt.extract(hdr.eth2);
        transition select(hdr.eth2.ethertype) {
ETHERTYPE_IPV4:
            prs_ipv4;
ETHERTYPE_IPV6:
            prs_ipv6;
        default:
            accept;
        }
    }



    state prs_ipv4 {
        pkt.extract(hdr.ipv4);
        ig_md.layer4_length = hdr.ipv4.total_len - 20;
        ig_md.ipv4_valid = 1;
        transition select(hdr.ipv4.protocol) {
IP_PROTOCOL_UDP:
            prs_udp;
IP_PROTOCOL_TCP:
            prs_tcp;
IP_PROTOCOL_IPV4:
            prs_ipv4b;
IP_PROTOCOL_IPV6:
            prs_ipv6b;
IP_PROTOCOL_SRL2:
            prs_eth3;
        default:
            accept;
        }
    }

    state prs_ipv6 {
        pkt.extract(hdr.ipv6);
        ig_md.layer4_length = hdr.ipv6.payload_len;
        ig_md.ipv6_valid = 1;
        transition select(hdr.ipv6.next_hdr) {
IP_PROTOCOL_UDP:
            prs_udp;
IP_PROTOCOL_TCP:
            prs_tcp;
IP_PROTOCOL_IPV4:
            prs_ipv4b;
IP_PROTOCOL_IPV6:
            prs_ipv6b;
IP_PROTOCOL_SRL2:
            prs_eth3;
        default:
            accept;
        }
    }


    state prs_udp {
        pkt.extract(hdr.udp);
        ig_md.layer4_srcprt = hdr.udp.src_port;
        ig_md.layer4_dstprt = hdr.udp.dst_port;
        transition accept;
    }

    state prs_tcp {
        pkt.extract(hdr.tcp);
        ig_md.layer4_srcprt = hdr.tcp.src_port;
        ig_md.layer4_dstprt = hdr.tcp.dst_port;
        transition accept;
    }

    state prs_eth3 {
        pkt.extract(hdr.eth3);
        transition accept;
    }

    state prs_ipv4b {
        pkt.extract(hdr.ipv4b);
        transition accept;
    }

    state prs_ipv6b {
        pkt.extract(hdr.ipv6b);
        transition accept;
    }

    state prs_arp {
        pkt.extract(hdr.arp);
        ig_md.arp_valid = 1;
        transition accept;
    }

    state prs_control {
        ig_md.llc_valid = 1;
        transition accept;
    }

    state prs_llc {
        pkt.extract(hdr.llc);
        ig_md.llc_valid = 1;
        transition select(hdr.llc.dsap, hdr.llc.ssap) {
            /*
             * (0xaa, 0xaa): prs_snap_header;
             * From switch.p4 this case should be processed.
             * We are not there yet :-)
             */
            (0xfe, 0xfe): accept;
        default:
            accept;
        }
    }

}

#endif // _INGRESS_PARSER_P4_
