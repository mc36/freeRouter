/*
 * Copyright 2019-present GT RARE project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed On an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _IG_CTL_MCAST_P4_
#define _IG_CTL_MCAST_P4_

#ifdef HAVE_MCAST

control IngressControlMcast(inout ingress_headers hdr, inout ingress_metadata_t ig_md,
                            in ingress_intrinsic_metadata_t ig_intr_md,
                            inout ingress_intrinsic_metadata_for_deparser_t ig_dprsr_md,
                            inout ingress_intrinsic_metadata_for_tm_t ig_tm_md)
{


    action act_local(SubIntId_t ingr, bit<16> sess) {
        ig_md.ipv4_valid = 0;
        ig_md.ipv6_valid = 0;
        ig_md.clone_session = sess;
        ig_md.rpf_iface = ingr;
    }

    action act_flood(SubIntId_t ingr, bit<16> sess) {
        ig_md.ipv4_valid = 0;
        ig_md.ipv6_valid = 0;
        ig_md.clone_session = sess;
        ig_md.rpf_iface = ingr;
        ig_tm_md.mcast_grp_a = sess;
        ig_tm_md.ucast_egress_port = MAX_PORT;
        ig_tm_md.bypass_egress = 0;
        hdr.vlan.setInvalid();
        hdr.ethernet.ethertype = ig_md.ethertype;
        hdr.cpu.setInvalid();
        hdr.internal.setValid();
        hdr.internal.reason = INTREAS_IPMCAST;
        hdr.internal.session = sess;
    }


    table tbl_mcast4 {
        key = {
ig_md.vrf:
            exact;
hdr.ipv4.src_addr:
            exact;
hdr.ipv4.dst_addr:
            exact;
        }
        actions = {
            act_local;
            act_flood;
            @defaultonly NoAction;
        }
        size = IPV4_MCAST_TABLE_SIZE;
        const default_action = NoAction();
    }


    table tbl_mcast6 {
        key = {
ig_md.vrf:
            exact;
hdr.ipv6.src_addr:
            exact;
hdr.ipv6.dst_addr:
            exact;
        }
        actions = {
            act_local;
            act_flood;
            @defaultonly NoAction;
        }
        size = IPV6_MCAST_TABLE_SIZE;
        const default_action = NoAction();
    }


    apply {

        if (ig_md.ipv4_valid==1)  {
            tbl_mcast4.apply();
        } else if (ig_md.ipv6_valid==1)  {
            tbl_mcast6.apply();
        }

        if ((ig_md.clone_session != 0) && (ig_md.rpf_iface != ig_md.source_id)) {
            ig_dprsr_md.drop_ctl = 1;
        }

    }


}

#endif

#endif // _IG_CTL_MCAST_P4_