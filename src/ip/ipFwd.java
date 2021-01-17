package ip;

import addr.addrIP;
import addr.addrPrefix;
import clnt.clntMplsTeP2p;
import clnt.clntNetflow;
import ifc.ifcEthTyp;
import ifc.ifcNshFwd;
import java.util.Comparator;
import java.util.List;
import pack.packHolder;
import prt.prtTcp;
import rtr.rtrLdpIface;
import rtr.rtrLdpNeigh;
import rtr.rtrLdpTrgtd;
import tab.tabAceslstN;
import tab.tabConnect;
import tab.tabGen;
import tab.tabLabel;
import tab.tabLabelBier;
import tab.tabLabelBierN;
import tab.tabLabelDup;
import tab.tabLabelNtry;
import tab.tabListing;
import tab.tabNatCfgN;
import tab.tabNatTraN;
import tab.tabPbrN;
import tab.tabPrfxlstN;
import tab.tabQos;
import tab.tabRoute;
import tab.tabRouteAttr;
import tab.tabRouteEntry;
import tab.tabRtrmapN;
import tab.tabRtrplcN;
import util.bits;
import util.counter;
import util.debugger;
import util.history;
import util.logger;
import util.notifier;
import util.state;

/**
 * does ip forwarding, services protocols
 *
 * @author matecsaba
 */
public class ipFwd implements Runnable, Comparator<ipFwd> {

    /**
     * the ip version
     */
    public final int ipVersion;

    /**
     * configured name of routing table
     */
    public final String cfgName;

    /**
     * name of routing table
     */
    public final String vrfName;

    /**
     * number of routing table
     */
    public final int vrfNum;

    /**
     * number of updates
     */
    public int updateCount;

    /**
     * update time took
     */
    public int updateTime;

    /**
     * time of update
     */
    public long updateLast;

    /**
     * number of changes
     */
    public int changeCount;

    /**
     * time of change
     */
    public long changeLast;

    /**
     * route distinguisher
     */
    public long rd;

    /**
     * multicast distribution tree
     */
    public boolean mdt;

    /**
     * current list of routers
     */
    public final tabGen<ipRtr> routers;

    /**
     * current list of protocols
     */
    public final tabConnect<addrIP, ipFwdProto> protos;

    /**
     * list of current interfaces
     */
    public final tabGen<ipFwdIface> ifaces;

    /**
     * the configured static unicast route table
     */
    public final tabGen<ipFwdRoute> staticU;

    /**
     * the configured static multicast route table
     */
    public final tabGen<ipFwdRoute> staticM;

    /**
     * the computed connected table
     */
    public tabRoute<addrIP> connedR;

    /**
     * the labeled table
     */
    public tabRoute<addrIP> labeldR;

    /**
     * the computed unicast routing table
     */
    public tabRoute<addrIP> actualU;

    /**
     * the computed multicast routing table
     */
    public tabRoute<addrIP> actualM;

    /**
     * the computed flowspec routing table
     */
    public tabRoute<addrIP> actualF;

    /**
     * list of multicast groups
     */
    public final tabGen<ipFwdMcast> groups;

    /**
     * the configured pbr entries
     */
    public final tabListing<tabPbrN, addrIP> pbrCfg;

    /**
     * the configured nat entries
     */
    public final tabListing<tabNatCfgN, addrIP> natCfg;

    /**
     * current nat entries
     */
    public final tabGen<tabNatTraN> natTrns;

    /**
     * current icmp sessions
     */
    public final tabGen<ipFwdEcho> echoes;

    /**
     * traffic engineering tunnels
     */
    public final tabGen<ipFwdTrfng> trafEngs;

    /**
     * multipoint label paths
     */
    public final tabGen<ipFwdMpmp> mp2mpLsp;

    /**
     * ldp neighbors
     */
    public final tabGen<rtrLdpNeigh> ldpNeighs;

    /**
     * targeted ldp neighbors
     */
    public final tabGen<rtrLdpTrgtd> ldpTarget;

    /**
     * auto mesh te neighbors
     */
    public final tabGen<clntMplsTeP2p> autoMesh;

    /**
     * total counter for this vrf
     */
    public counter cntrT;

    /**
     * total historic for this vrf
     */
    public history hstryT;

    /**
     * local counter for this vrf
     */
    public counter cntrL;

    /**
     * local historic for this vrf
     */
    public history hstryL;

    /**
     * netflow exporter
     */
    public clntNetflow netflow;

    /**
     * allocate label for which prefix
     */
    public labelMode prefixMode = labelMode.common;

    /**
     * mpls propagate ip ttl
     */
    public boolean mplsPropTtl = true;

    /**
     * mpls extended report
     */
    public boolean mplsExtRep = true;

    /**
     * unreachable last
     */
    public long unreachLst = 0;

    /**
     * unreachable interval
     */
    public int unreachInt = 0;

    /**
     * ruin remote pmtud
     */
    public boolean ruinPmtuD = false;

    /**
     * label allocation filter
     */
    public tabListing<tabPrfxlstN, addrIP> labelFilter;

    /**
     * packet forwarding filter
     */
    public tabListing<tabAceslstN<addrIP>, addrIP> packetFilter;

    /**
     * data plane qos
     */
    public tabQos dapp;

    /**
     * flowspec qos
     */
    public tabQos flowspec;

    /**
     * receive control plane qos
     */
    public tabQos coppIn;

    /**
     * transmit control plane qos
     */
    public tabQos coppOut;

    /**
     * traffic counter filter
     */
    public tabListing<tabRtrmapN, addrIP> counterMap;

    /**
     * import list
     */
    public tabListing<tabPrfxlstN, addrIP> importList;

    /**
     * export list
     */
    public tabListing<tabPrfxlstN, addrIP> exportList;

    /**
     * import map
     */
    public tabListing<tabRtrmapN, addrIP> importMap;

    /**
     * export map
     */
    public tabListing<tabRtrmapN, addrIP> exportMap;

    /**
     * import policy
     */
    public tabListing<tabRtrplcN, addrIP> importPol;

    /**
     * export policy
     */
    public tabListing<tabRtrplcN, addrIP> exportPol;

    /**
     * time when recompute automatically
     */
    public int untriggeredRecomputation = 120 * 1000;

    /**
     * notify when table changed
     */
    public notifier tableChanged;

    /**
     * common label
     */
    public tabLabelNtry commonLabel;

    /**
     * ip core to use
     */
    protected final ipCor ipCore;

    /**
     * icmp core to use
     */
    public final ipIcmp icmpCore;

    /**
     * igmp/mmld core to use
     */
    public final ipMhost mhostCore;

    private notifier triggerUpdate;

    private static int nextVrfNumber = bits.randomD();

    private static int nextRouterNumber = bits.randomD();

    private int nextIfaceNumber = bits.randomD();

    private int nextEchoNumber = bits.randomD();

    /**
     * label modes
     */
    public enum labelMode {

        /**
         * common label for vrf
         */
        common,
        /**
         * label for host routes
         */
        host,
        /**
         * label for igp prefixes
         */
        igp,
        /**
         * label for all prefix
         */
        all

    }

    public String toString() {
        return vrfName;
    }

    public int compare(ipFwd o1, ipFwd o2) {
        if (o1.vrfNum < o2.vrfNum) {
            return -1;
        }
        if (o1.vrfNum > o2.vrfNum) {
            return +1;
        }
        if (o1.ipVersion < o2.ipVersion) {
            return -1;
        }
        if (o1.ipVersion > o2.ipVersion) {
            return +1;
        }
        return 0;
    }

    /**
     * the constructor of vrf
     *
     * @param ipc handler of ip core
     * @param icc handler of icmp core
     * @param mhst handler of igmp/mdl core
     * @param cfg configured name of this vrf
     * @param nam name of this vrf
     */
    public ipFwd(ipCor ipc, ipIcmp icc, ipMhost mhst, String cfg, String nam) {
        nextVrfNumber = (nextVrfNumber & 0x3fffffff) + 1;
        cfgName = cfg;
        vrfName = nam;
        vrfNum = nextVrfNumber + 10000;
        ipCore = ipc;
        icmpCore = icc;
        mhostCore = mhst;
        ipVersion = ipCore.getVersion();
        commonLabel = tabLabel.allocate(1);
        echoes = new tabGen<ipFwdEcho>();
        trafEngs = new tabGen<ipFwdTrfng>();
        mp2mpLsp = new tabGen<ipFwdMpmp>();
        ifaces = new tabGen<ipFwdIface>();
        groups = new tabGen<ipFwdMcast>();
        protos = new tabConnect<addrIP, ipFwdProto>(new addrIP(), "protocols");
        routers = new tabGen<ipRtr>();
        ldpNeighs = new tabGen<rtrLdpNeigh>();
        ldpTarget = new tabGen<rtrLdpTrgtd>();
        autoMesh = new tabGen<clntMplsTeP2p>();
        connedR = new tabRoute<addrIP>("conn");
        labeldR = new tabRoute<addrIP>("labeled");
        actualU = new tabRoute<addrIP>("computed");
        actualM = new tabRoute<addrIP>("computed");
        actualF = new tabRoute<addrIP>("computed");
        staticU = new tabGen<ipFwdRoute>();
        staticM = new tabGen<ipFwdRoute>();
        natTrns = new tabGen<tabNatTraN>();
        pbrCfg = new tabListing<tabPbrN, addrIP>();
        pbrCfg.myCor = ipCore;
        pbrCfg.myIcmp = icc;
        natCfg = new tabListing<tabNatCfgN, addrIP>();
        natCfg.myCor = ipCore;
        natCfg.myIcmp = icc;
        cntrT = new counter();
        hstryT = new history();
        cntrL = new counter();
        hstryL = new history();
        triggerUpdate = new notifier();
        ipFwdTab.updateEverything(this);
        icc.setForwarder(this);
        mhst.setForwarder(this, icc);
        new Thread(this).start();
    }

    /**
     * stop this routing table completely
     */
    public void stopThisVrf() {
        untriggeredRecomputation = -1;
        triggerUpdate.wakeup();
        prefixMode = labelMode.common;
        for (int i = 0; i < labeldR.size(); i++) {
            tabRouteEntry<addrIP> ntry = labeldR.get(i);
            if (ntry == null) {
                continue;
            }
            tabLabel.release(ntry.best.labelLoc, 2);
        }
        for (int i = 0; i < trafEngs.size(); i++) {
            ipFwdTrfng ntry = trafEngs.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.labStop();
        }
        for (int i = 0; i < mp2mpLsp.size(); i++) {
            ipFwdMpmp ntry = mp2mpLsp.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.stopLabels();
        }
        for (int i = ldpNeighs.size() - 1; i >= 0; i--) {
            rtrLdpNeigh ntry = ldpNeighs.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.stopPeer();
        }
        for (int i = ldpTarget.size() - 1; i >= 0; i--) {
            rtrLdpTrgtd ntry = ldpTarget.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.workStop();
        }
        for (int i = routers.size() - 1; i >= 0; i--) {
            ipRtr rtr = routers.get(i);
            if (rtr == null) {
                continue;
            }
            routerDel(rtr);
        }
        for (int i = 0; i < autoMesh.size(); i++) {
            clntMplsTeP2p ntry = autoMesh.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.workStop();
        }
        tabLabel.release(commonLabel, 1);
        for (int i = ifaces.size() - 1; i >= 0; i--) {
            ipFwdIface ifc = ifaces.get(i);
            if (ifc == null) {
                continue;
            }
            ifaceDel(ifc);
        }
    }

    /**
     * wake up when tables changed
     */
    public void tableChanger() {
        if (tableChanged == null) {
            return;
        }
        tableChanged.wakeup();
    }

    /**
     * find one ldp neighbor
     *
     * @param iface receiving interface
     * @param addr peer address
     * @param create create if not yet
     * @return found neighbor, null if nothing
     */
    public rtrLdpNeigh ldpNeighFind(ipFwdIface iface, addrIP addr, boolean create) {
        if (iface != null) {
            iface = ifaces.find(iface);
            if (iface == null) {
                return null;
            }
            tabRouteEntry<addrIP> route = connedR.route(addr);
            if (route == null) {
                return null;
            }
        }
        rtrLdpNeigh ntry = new rtrLdpNeigh(addr);
        if (!create) {
            return ldpNeighs.find(ntry);
        }
        ntry.ifc = iface;
        ntry.ip = this;
        rtrLdpNeigh old = ldpNeighs.add(ntry);
        if (old != null) {
            return old;
        }
        return ntry;
    }

    /**
     * delete one ldp neighbor
     *
     * @param ntry entry to delete
     */
    public void ldpNeighDel(rtrLdpNeigh ntry) {
        rtrLdpNeigh old = ldpNeighs.del(ntry);
        if (old != null) {
            old.stopPeer();
        }
        ntry.stopPeer();
        triggerUpdate.wakeup();
    }

    /**
     * find one ldp targeted
     *
     * @param iface receiving interface
     * @param ldpi ldp interface
     * @param addr peer address
     * @param create create if not yet
     * @return found neighbor, null if nothing
     */
    public rtrLdpTrgtd ldpTargetFind(ipFwdIface iface, rtrLdpIface ldpi, addrIP addr, boolean create) {
        iface = ifaces.find(iface);
        if (iface == null) {
            return null;
        }
        rtrLdpTrgtd ntry = new rtrLdpTrgtd(addr);
        if (!create) {
            return ldpTarget.find(ntry);
        }
        ntry.ifc = iface;
        ntry.ip = this;
        ntry.ldp = ldpi;
        rtrLdpTrgtd old = ldpTarget.add(ntry);
        if (old != null) {
            return old;
        }
        return ntry;
    }

    /**
     * delete one ldp targeted
     *
     * @param ntry entry to delete
     */
    public void ldpTargetDel(rtrLdpTrgtd ntry) {
        ntry = ldpTarget.del(ntry);
        if (ntry != null) {
            ntry.workStop();
        }
        triggerUpdate.wakeup();
    }

    /**
     * add label to flood list
     *
     * @param grp group to flood
     * @param src source of group
     * @param trg label to flood
     */
    public void mcastAddFloodMpls(addrIP grp, addrIP src, ipFwdMpmp trg) {
        ipFwdMcast g = new ipFwdMcast(grp, src);
        if (trg == null) {
            ipFwdMcast og = groups.find(g);
            if (og == null) {
                return;
            }
            og.label = null;
            return;
        }
        ipFwdMcast og = groups.add(g);
        if (og != null) {
            g = og;
        } else {
            ipFwdTab.updateOneGroup(this, g);
            ipFwdTab.joinOneGroup(this, g, 1);
        }
        g.label = trg;
    }

    /**
     * add bier peer to flood list
     *
     * @param grp group to flood
     * @param src source of group
     * @param trg peer address
     * @param id local node id
     * @param typ ethertype
     * @param exp expiration time, negative if not expires
     */
    public void mcastAddFloodBier(addrIP grp, addrIP src, addrIP trg, int id, int typ, long exp) {
        ipFwdMcast g = new ipFwdMcast(grp, src);
        ipFwdMcast og = groups.add(g);
        if (og != null) {
            g = og;
        } else {
            ipFwdTab.updateOneGroup(this, g);
            ipFwdTab.joinOneGroup(this, g, 1);
        }
        ipFwdBier ntry = g.bier;
        if (ntry == null) {
            ntry = new ipFwdBier();
            ntry.fwd = this;
            ntry.id = id;
            ntry.typ = typ;
            g.bier = ntry;
            ntry.workStart();
        }
        ntry.addPeer(trg, exp);
        tableChanger();
    }

    /**
     * del bier peer from flood list
     *
     * @param grp group to flood
     * @param src source of group
     * @param trg peer address
     */
    public void mcastDelFloodBier(addrIP grp, addrIP src, addrIP trg) {
        ipFwdMcast g = new ipFwdMcast(grp, src);
        g = groups.find(g);
        if (g == null) {
            return;
        }
        if (g.bier == null) {
            return;
        }
        g.bier.delPeer(trg);
        tableChanger();
    }

    /**
     * add interface to flood list
     *
     * @param grp group to flood
     * @param src source of group
     * @param ifc interface to add, null=local
     * @param exp expiration time, negative if not expires, -1=static,
     * -2=globalCfg, -3=ifaceCfg
     */
    public void mcastAddFloodIfc(addrIP grp, addrIP src, ipFwdIface ifc, long exp) {
        if (exp > 0) {
            exp += bits.getTime();
        }
        ipFwdMcast g = new ipFwdMcast(grp, src);
        ipFwdMcast og = groups.add(g);
        if (og != null) {
            g = og;
        } else {
            ipFwdTab.updateOneGroup(this, g);
            ipFwdTab.joinOneGroup(this, g, 1);
        }
        g.configG = exp == -2;
        g.configI = exp == -3;
        if (ifc == null) {
            g.local = true;
            return;
        }
        ipFwdIface oi = g.flood.add(ifc);
        tableChanger();
        if (oi != null) {
            ifc = oi;
        }
        if (ifc.expires < 0) {
            return;
        }
        ifc.expires = exp;
    }

    /**
     * del interface from flood list
     *
     * @param grp group to flood
     * @param src source of group
     * @param ifc interface to add, null=local
     */
    public void mcastDelFloodIfc(addrIP grp, addrIP src, ipFwdIface ifc) {
        ipFwdMcast g = new ipFwdMcast(grp, src);
        g = groups.find(g);
        if (g == null) {
            return;
        }
        if (ifc == null) {
            g.local = false;
            g.configG = false;
            return;
        }
        g.configI = false;
        g.flood.del(ifc);
        tableChanger();
    }

    /**
     * add local tunnel
     *
     * @param ntry tunnel entry
     * @param p2mp set true for point to multipoint
     */
    public void tetunAdd(ipFwdTrfng ntry, boolean p2mp) {
        ntry.srcLoc = 1;
        ntry.trgLab = -1;
        ntry.timeout = 3 * untriggeredRecomputation;
        if (p2mp) {
            trafEngs.put(ntry);
            ipFwdTab.refreshTrfngAdd(this, ntry);
            return;
        }
        for (;;) {
            ntry.srcId = bits.randomW();
            if (trafEngs.add(ntry) == null) {
                break;
            }
        }
        ipFwdTab.refreshTrfngAdd(this, ntry);
    }

    /**
     * del local tunnel
     *
     * @param ntry tunnel entry
     */
    public void tetunDel(ipFwdTrfng ntry) {
        ntry = trafEngs.del(ntry);
        if (ntry == null) {
            return;
        }
        if (ntry.srcLoc == 0) {
            return;
        }
        ntry.srcLoc = 2;
        ntry.labStop();
        ipFwdTab.refreshTrfngDel(this, ntry);
    }

    /**
     * refresh local tunnel
     *
     * @param ntry tunnel entry
     */
    public void tetunSignal(ipFwdTrfng ntry) {
        ntry = trafEngs.find(ntry);
        if (ntry == null) {
            return;
        }
        if (ntry.srcLoc == 0) {
            return;
        }
        ipFwdTab.refreshTrfngAdd(this, ntry);
    }

    /**
     * add mldp tunnel
     *
     * @param ntry tunnel entry
     */
    public void mldpAdd(ipFwdMpmp ntry) {
        ntry.local = true;
        ipFwdMpmp old = mp2mpLsp.add(ntry);
        if (old != null) {
            ntry = old;
        }
        ntry.local = true;
        ntry.updateState(this);
    }

    /**
     * del mldp tunnel
     *
     * @param ntry tunnel entry
     */
    public void mldpDel(ipFwdMpmp ntry) {
        ntry = mp2mpLsp.find(ntry);
        if (ntry == null) {
            return;
        }
        ntry.local = false;
        ntry.updateState(this);
    }

    /**
     * del static route
     *
     * @param uni true=unicast, false=multicast
     * @param rou route
     */
    public void staticDel(boolean uni, ipFwdRoute rou) {
        if (uni) {
            rou = staticU.del(rou);
        } else {
            rou = staticM.del(rou);
        }
        if (rou != null) {
            if (rou.track != null) {
                rou.track.clients.del(this);
            }
        }
        triggerUpdate.wakeup();
    }

    /**
     * add static route
     *
     * @param uni true=unicast, false=multicast
     * @param rou route
     */
    public void staticAdd(boolean uni, ipFwdRoute rou) {
        rou.fwdCor = this;
        if (uni) {
            staticU.add(rou);
        } else {
            staticM.add(rou);
        }
        if (rou.track != null) {
            rou.track.clients.add(this);
        }
        triggerUpdate.wakeup();
    }

    /**
     * add one interface
     *
     * @param lower interface to add
     * @return interface handler
     */
    public ipFwdIface ifaceAdd(ipIfc lower) {
        if (debugger.ipFwdEvnt) {
            logger.debug("add ifc " + lower);
        }
        ipFwdIface ntry;
        for (;;) {
            nextIfaceNumber = (nextIfaceNumber & 0x3fffffff) + 1;
            ntry = new ipFwdIface(nextIfaceNumber + 10000, lower);
            ntry.addr = new addrIP();
            ntry.network = new addrPrefix<addrIP>(ntry.addr, ntry.addr.maxBits());
            ntry.ready = true;
            ntry.mtu = lower.getMTUsize() - ipCore.getHeaderSize();
            ntry.bandwidth = lower.getBandwidth();
            if (ifaces.add(ntry) == null) {
                break;
            }
        }
        lower.setUpper(this, ntry);
        triggerUpdate.wakeup();
        return ntry;
    }

    /**
     * delete one interface
     *
     * @param ifc interface handler
     */
    public void ifaceDel(ipFwdIface ifc) {
        ifc = ifaces.del(ifc);
        if (ifc == null) {
            return;
        }
        if (debugger.ipFwdEvnt) {
            logger.debug("del ifc " + ifc.lower);
        }
        ifc.ready = false;
        for (;;) {
            ipFwdProto prt = protos.delNext(ifc.ifwNum, null, 0, 0);
            if (prt == null) {
                break;
            }
            prt.upper.closeUp(ifc);
        }
        for (int i = 0; i < groups.size(); i++) {
            ipFwdMcast grp = groups.get(i);
            if (grp == null) {
                continue;
            }
            grp.flood.del(ifc);
        }
        triggerUpdate.wakeup();
    }

    /**
     * change interface state
     *
     * @param ifc interface handler
     * @param stat new status of interface
     */
    public void ifaceState(ipFwdIface ifc, state.states stat) {
        ifc.cntr.stateChange(stat);
        boolean old = ifc.ready;
        ifc.ready = (stat == state.states.up);
        ifc.mtu = ifc.lower.getMTUsize() - ipCore.getHeaderSize();
        ifc.bandwidth = ifc.lower.getBandwidth();
        if (old == ifc.ready) {
            return;
        }
        if (debugger.ipFwdEvnt) {
            logger.debug("iface state " + ifc.ready + " " + ifc.lower);
        }
        for (int i = protos.size() - 1; i >= 0; i--) {
            ipFwdProto prt = protos.get(i);
            if (prt == null) {
                continue;
            }
            if ((prt.iface == 0) || (prt.iface == ifc.ifwNum)) {
                prt.upper.setState(ifc, stat);
            }
        }
        triggerUpdate.wakeup();
    }

    /**
     * change interface address
     *
     * @param ifc interface handler
     * @param addr new address
     * @param mask net netmask
     */
    public void ifaceAddr(ipFwdIface ifc, addrIP addr, int mask) {
        if (debugger.ipFwdEvnt) {
            logger.debug("iface addr " + addr + " " + mask);
        }
        ifc.addr = addr.copyBytes();
        ifc.mask = mask;
        ifc.network = new addrPrefix<addrIP>(addr, mask);
        ifc.point2point = mask >= (addrIP.size * 8 - 1);
        triggerUpdate.wakeup();
    }

    private void ifaceProto(ipFwdIface lower, packHolder pck, addrIP hop) {
        cntrT.tx(pck);
        if (!lower.ready) {
            lower.cntr.drop(pck, counter.reasons.notUp);
            return;
        }
        pck.putStart();
        if (hop == null) {
            hop = pck.IPtrg;
            if (hop == null) {
                lower.cntr.drop(pck, counter.reasons.badAddr);
                return;
            }
        }
        if (!lower.point2point) {
            if (lower.blockBroadcast && (pck.INTupper == 0)) {
                if (pck.IPtrg.compare(pck.IPtrg, lower.network.network) == 0) {
                    lower.cntr.drop(pck, counter.reasons.denied);
                    return;
                }
                if (pck.IPtrg.compare(pck.IPtrg, lower.network.broadcast) == 0) {
                    lower.cntr.drop(pck, counter.reasons.denied);
                    return;
                }
            }
        }
        if (lower.blockHost2host && (pck.INTupper == 0)) {
            if (pck.INTiface == lower.ifwNum) {
                lower.cntr.drop(pck, counter.reasons.denied);
                return;
            }
        }
        lower.cntr.tx(pck);
        if (lower.cfilterOut != null) {
            if (!lower.cfilterOut.matches(false, true, pck)) {
                doDrop(pck, lower, counter.reasons.denied);
                return;
            }
        }
        if (lower.filterOut != null) {
            if (!lower.filterOut.matches(false, true, pck)) {
                doDrop(pck, lower, counter.reasons.denied);
                return;
            }
        }
        if (lower.tcpMssOut > 0) {
            ifaceAdjustMss(pck, lower.tcpMssOut);
        }
        if (debugger.ipFwdTraf) {
            logger.debug("tx " + pck.IPsrc + " -> " + pck.IPtrg + " hop=" + hop + " pr=" + pck.IPprt + " tos=" + pck.IPtos);
        }
        if (lower.inspect != null) {
            if (lower.inspect.doPack(pck, true)) {
                return;
            }
        }
        lower.lower.sendProto(pck, hop);
    }

    private void ifaceMpls(ipFwdIface lower, packHolder pck, addrIP hop) {
        cntrT.tx(pck);
        if (!lower.ready) {
            lower.cntr.drop(pck, counter.reasons.notUp);
            return;
        }
        pck.putStart();
        if (hop == null) {
            lower.cntr.drop(pck, counter.reasons.badAddr);
            return;
        }
        if (debugger.ipFwdTraf) {
            logger.debug("tx label=" + hop);
        }
        lower.lower.sendMpls(pck, hop);
    }

    private void ifaceAdjustMss(packHolder pck, int mss) {
        if (pck.IPprt != prtTcp.protoNum) {
            return;
        }
        pck.getSkip(pck.IPsiz);
        prtTcp.parseTCPports(pck);
        if ((pck.TCPflg & prtTcp.flagSYN) == 0) {
            pck.getSkip(-pck.IPsiz);
            return;
        }
        prtTcp.updateTCPheader(pck, pck.UDPsrc, pck.UDPtrg, -1, -1, mss);
        pck.getSkip(-pck.IPsiz);
        ipCore.updateIPheader(pck, pck.IPsrc, pck.IPtrg, -1, -1, -1, pck.UDPsiz);
    }

    /**
     * interface signals that it got a packet
     *
     * @param lower interface handler
     * @param pck packet to process
     */
    public void ifacePack(ipFwdIface lower, packHolder pck) {
        if (lower == null) {
            cntrT.drop(pck, counter.reasons.noIface);
            return;
        }
        if (!lower.ready) {
            lower.cntr.drop(pck, counter.reasons.notUp);
            return;
        }
        if (ipCore.parseIPheader(pck, true)) {
            lower.cntr.drop(pck, counter.reasons.badHdr);
            return;
        }
        if (lower.cfilterIn != null) {
            if (!lower.cfilterIn.matches(false, true, pck)) {
                doDrop(pck, lower, counter.reasons.denied);
                return;
            }
        }
        if (lower.filterIn != null) {
            if (!lower.filterIn.matches(false, true, pck)) {
                doDrop(pck, lower, counter.reasons.denied);
                return;
            }
        }
        pck.putStart();
        pck.INTiface = lower.ifwNum;
        if (lower.verifySource && !pck.IPlnk) {
            tabRouteEntry<addrIP> prf = actualU.route(pck.IPsrc);
            if (prf == null) {
                lower.cntr.drop(pck, counter.reasons.denied);
                return;
            }
            if ((lower.verifyStricht) && (prf.best.iface != lower)) {
                lower.cntr.drop(pck, counter.reasons.denied);
                return;
            }
        }
        if (debugger.ipFwdTraf) {
            logger.debug("rx " + pck.IPsrc + " -> " + pck.IPtrg + " pr=" + pck.IPprt + " tos=" + pck.IPtos);
        }
        if (lower.tcpMssIn > 0) {
            ifaceAdjustMss(pck, lower.tcpMssIn);
        }
        if (lower.inspect != null) {
            if (lower.inspect.doPack(pck, false)) {
                return;
            }
        }
        pck.INTupper = 0;
        ipMpls.beginMPLSfields(pck, (mplsPropTtl | lower.mplsPropTtlAlways) & lower.mplsPropTtlAllow);
        if (doPbrFwd(lower.pbrCfg, 1, lower, pck)) {
            return;
        }
        forwardPacket(1, lower, null, pck);
    }

    /**
     * add protocol to interface
     *
     * @param upper protocol handler
     * @param ifc interface handle, null for all
     * @param trg target address, null means all
     * @return true if error happened, false if success
     */
    public boolean protoAdd(ipPrt upper, ipFwdIface ifc, addrIP trg) {
        int iface = 0;
        if (ifc != null) {
            ifc = ifaces.find(ifc);
            if (ifc == null) {
                return true;
            }
            iface = ifc.ifwNum;
        }
        if (debugger.ipFwdEvnt) {
            logger.debug("add proto=" + upper + " iface=" + iface + " trg=" + trg);
        }
        ipFwdProto ntry = new ipFwdProto();
        ntry.proto = upper.getProtoNum();
        ntry.iface = iface;
        ntry.upper = upper;
        return protos.add(iface, trg, ntry.proto, ntry.proto, ntry, "" + upper);
    }

    /**
     * delete protocol from interface
     *
     * @param upper protocol handler
     * @param ifc interface handle, null for all
     * @param trg target address, null means all
     */
    public void protoDel(ipPrt upper, ipFwdIface ifc, addrIP trg) {
        int iface = ipFwdIface.getNum(ifc);
        if (debugger.ipFwdEvnt) {
            logger.debug("del proto=" + upper + " iface=" + iface + " trg=" + trg);
        }
        int i = upper.getProtoNum();
        for (;;) {
            ipFwdProto prt = protos.delNext(iface, trg, i, i);
            if (prt == null) {
                break;
            }
        }
    }

    private void protoSend(ipFwdIface lower, packHolder pck) {
        cntrL.rx(pck);
        if ((pck.IPmf) || (pck.IPfrg != 0)) {
            if (ruinPmtuD) {
                doDrop(pck, lower, counter.reasons.fragment);
            } else {
                lower.cntr.drop(pck, counter.reasons.fragment);
            }
            return;
        }
        if (coppIn != null) {
            if (coppIn.checkPacket(bits.getTime(), pck)) {
                cntrL.drop(pck, counter.reasons.noBuffer);
                return;
            }
        }
        if (debugger.ipFwdTraf) {
            logger.debug("rcv " + pck.IPsrc + " -> " + pck.IPtrg + " pr=" + pck.IPprt + " tos=" + pck.IPtos);
        }
        ipFwdProto prt = null;
        if (prt == null) {
            prt = protos.get(lower.ifwNum, pck.IPsrc, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            prt = protos.get(0, pck.IPsrc, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            prt = protos.get(lower.ifwNum, null, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            prt = protos.get(0, null, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            doDrop(pck, lower, counter.reasons.badProto);
            return;
        }
        pck.getSkip(pck.IPsiz);
        prt.upper.recvPack(lower, pck);
    }

    private boolean protoAlert(ipFwdIface lower, packHolder pck) {
        cntrL.rx(pck);
        if ((pck.IPmf) || (pck.IPfrg != 0)) {
            return true;
        }
        if (coppIn != null) {
            if (coppIn.checkPacket(bits.getTime(), pck)) {
                cntrL.drop(pck, counter.reasons.noBuffer);
                return false;
            }
        }
        if (debugger.ipFwdTraf) {
            logger.debug("alrt " + pck.IPsrc + " -> " + pck.IPtrg + " pr=" + pck.IPprt + " tos=" + pck.IPtos);
        }
        ipFwdProto prt = null;
        if (prt == null) {
            prt = protos.get(lower.ifwNum, pck.IPsrc, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            prt = protos.get(0, pck.IPsrc, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            prt = protos.get(lower.ifwNum, null, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            prt = protos.get(0, null, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            return true;
        }
        pck.getSkip(pck.IPsiz);
        boolean b = prt.upper.alertPack(lower, pck);
        if (b) {
            pck.getSkip(-pck.IPsiz);
            return true;
        }
        return false;
    }

    /**
     * protocol wants to create one packet
     *
     * @param pck packet to update
     */
    public void createIPheader(packHolder pck) {
        pck.INTiface = -1;
        pck.INTupper = pck.IPprt;
        pck.merge2beg();
        ipCore.createIPheader(pck);
        if (debugger.ipFwdTraf) {
            logger.debug("snd " + pck.IPsrc + " -> " + pck.IPtrg + " pr=" + pck.IPprt + " tos=" + pck.IPtos);
        }
        ipCore.testIPaddress(pck, pck.IPtrg);
        ipMpls.beginMPLSfields(pck, mplsPropTtl);
    }

    /**
     * protocol wants to update one packet
     *
     * @param pck packet to update
     * @param src new source address, null=don't set
     * @param trg new target address, null=don't set
     * @param prt new protocol value, -1=dont set
     * @param ttl new ttl value, -1=dont set, -2=decrement
     * @param tos new tos value, -1=dont set
     * @param len new payload length, -1=dont set
     */
    public void updateIPheader(packHolder pck, addrIP src, addrIP trg, int prt, int ttl, int tos, int len) {
        pck.INTiface = -1;
        pck.INTupper = pck.IPprt;
        ipCore.updateIPheader(pck, src, trg, prt, ttl, tos, len);
        ipMpls.beginMPLSfields(pck, mplsPropTtl);
    }

    /**
     * protocol wants to send one packet
     *
     * @param iface interface to use for source address
     * @param hop forced nexthop
     * @param pck packet to send
     */
    public void protoPack(ipFwdIface iface, addrIP hop, packHolder pck) {
        cntrL.tx(pck);
        if (iface == null) {
            cntrL.drop(pck, counter.reasons.noIface);
            return;
        }
        if (!iface.ready) {
            iface.cntr.drop(pck, counter.reasons.notUp);
            return;
        }
        pck.INTiface = iface.ifwNum;
        pck.INTupper = pck.IPprt;
        pck.merge2beg();
        ipCore.createIPheader(pck);
        if (coppOut != null) {
            if (coppOut.checkPacket(bits.getTime(), pck)) {
                cntrL.drop(pck, counter.reasons.noBuffer);
                return;
            }
        }
        if (debugger.ipFwdTraf) {
            logger.debug("snd " + pck.IPsrc + " -> " + pck.IPtrg + " pr=" + pck.IPprt + " tos=" + pck.IPtos);
        }
        ipCore.testIPaddress(pck, pck.IPtrg);
        ipMpls.beginMPLSfields(pck, (mplsPropTtl | iface.mplsPropTtlAlways) & iface.mplsPropTtlAllow);
        forwardPacket(4, iface, hop, pck);
    }

    /**
     * add one routing protocol
     *
     * @param rtr routing protocol handle
     * @param typ route type that it provides
     * @param id process id
     */
    public void routerAdd(ipRtr rtr, tabRouteAttr.routeType typ, int id) {
        if (debugger.ipFwdEvnt) {
            logger.debug("add rtr " + rtr);
        }
        nextRouterNumber = (nextRouterNumber & 0x3fffffff) + 1;
        rtr.routerProtoNum = nextRouterNumber + 10000;
        rtr.routerProtoTyp = typ;
        rtr.routerProcNum = id;
        routers.add(rtr);
        triggerUpdate.wakeup();
    }

    /**
     * delete one routing protocol
     *
     * @param rtr routing protocol handle
     */
    public void routerDel(ipRtr rtr) {
        if (debugger.ipFwdEvnt) {
            logger.debug("del rtr " + rtr);
        }
        if (routers.del(rtr) == null) {
            return;
        }
        triggerUpdate.wakeup();
        rtr.routerCloseNow();
    }

    /**
     * routing protocol notified that change happened
     *
     * @param rtr routing protocol handle
     */
    public void routerChg(ipRtr rtr) {
        if (debugger.ipFwdEvnt) {
            logger.debug("chgd rtr " + rtr);
        }
        if (routers.find(rtr) == null) {
            return;
        }
        rtr.routerComputeChg++;
        rtr.routerComputeTim = bits.getTime();
        triggerUpdate.wakeup();
    }

    /**
     * static route change happened
     */
    public void routerStaticChg() {
        triggerUpdate.wakeup();
    }

    /**
     * send unreachable
     *
     * @param pck packet to report
     * @param lower interface
     * @param reason reason
     */
    public void doDrop(packHolder pck, ipFwdIface lower, counter.reasons reason) {
        cntrT.drop(pck, reason);
        if (unreachInt > 0) {
            long tim = bits.getTime();
            if ((tim - unreachLst) < unreachInt) {
                return;
            }
            unreachLst = tim;
        }
        if (debugger.ipFwdTraf) {
            logger.debug("drop " + pck.IPsrc + " -> " + pck.IPtrg + " pr=" + pck.IPprt + " reason=" + counter.reason2string(reason));
        }
        if (pck.IPmlt || pck.IPbrd) {
            return;
        }
        if (lower == null) {
            return;
        }
        addrIP src = lower.getUnreachAddr();
        if (src == null) {
            return;
        }
        if (icmpCore.createError(pck, reason, src.copyBytes(), mplsExtRep)) {
            return;
        }
        ipCore.createIPheader(pck);
        if (coppOut != null) {
            if (coppOut.checkPacket(bits.getTime(), pck)) {
                cntrL.drop(pck, counter.reasons.noBuffer);
                return;
            }
        }
        pck.INTupper = -1;
        ipMpls.beginMPLSfields(pck, (mplsPropTtl | lower.mplsPropTtlAlways) & lower.mplsPropTtlAllow);
        forwardPacket(4, lower, null, pck);
    }

    private void doMpls(ipFwdIface ifc, addrIP hop, List<Integer> labs, packHolder pck) {
        if (ifc == null) {
            cntrT.drop(pck, counter.reasons.noIface);
            return;
        }
        if (labs != null) {
            ipMpls.createMPLSlabels(pck, labs);
        }
        if (pck.MPLSbottom) {
            ifaceProto(ifc, pck, hop);
            return;
        }
        ifaceMpls(ifc, pck, hop);
    }

    /**
     * mpls signals that it got a packet
     *
     * @param fwd4 ipv4 forwarder
     * @param fwd6 ipv6 forwarder
     * @param fwdE ethernet forwarder
     * @param lab local label
     * @param pck packet to process
     */
    protected void mplsRxPack(ipFwd fwd4, ipFwd fwd6, ifcEthTyp fwdE, tabLabelNtry lab, packHolder pck) {
        if (debugger.ipFwdTraf) {
            logger.debug("rx label=" + lab.label);
        }
        pck.MPLSttl--;
        if (pck.MPLSttl < 2) {
            if (ipMpls.createError(pck, lab, counter.reasons.ttlExceed)) {
                return;
            }
        }
        if (lab.nextHop != null) {
            if ((lab.remoteLab == null) && (!pck.MPLSbottom)) {
                logger.info("no label for " + lab.label);
                cntrT.drop(pck, counter.reasons.notInTab);
                return;
            }
            doMpls(lab.iface, lab.nextHop, lab.remoteLab, pck);
            return;
        }
        if (lab.duplicate != null) {
            for (int i = 0; i < lab.duplicate.size(); i++) {
                tabLabelDup ntry = lab.duplicate.get(i);
                if (ntry == null) {
                    continue;
                }
                doMpls(ntry.ifc, ntry.hop, ntry.lab, pck.copyBytes(true, true));
            }
            if (!lab.needLocal) {
                return;
            }
        }
        if (lab.bier != null) {
            pck.BIERsi = lab.label - lab.bier.base;
            pck.BIERbsl = lab.bier.bsl;
            if (ipMpls.parseBIERheader(pck)) {
                logger.info("received invalid bier header on label " + lab.label);
                cntrT.drop(pck, counter.reasons.badHdr);
                return;
            }
            int bsl = tabLabelBier.bsl2num(lab.bier.bsl);
            int sis = bsl * pck.BIERsi;
            boolean nedLoc = tabLabelBier.untestMine(pck.BIERbs, bsl, lab.bier.idx - 1 - sis);
            nedLoc |= tabLabelBier.untestMine(pck.BIERbs, bsl, lab.bier.idx2 - 1 - sis);
            for (int i = 0; i < lab.bier.peers.size(); i++) {
                tabLabelBierN ntry = lab.bier.peers.get(i);
                if (ntry == null) {
                    continue;
                }
                packHolder p = pck.copyBytes(true, true);
                p.BIERbs = ntry.getAndShr(pck.BIERbs, sis);
                if (p.BIERbs == null) {
                    continue;
                }
                if (p.BIERbs.length < 1) {
                    continue;
                }
                ipMpls.createBIERheader(p);
                p.MPLSlabel = ntry.lab + pck.BIERsi;
                ipMpls.createMPLSheader(p);
                doMpls(ntry.ifc, ntry.hop, null, p);
            }
            if (nedLoc) {
                if (ipMpls.gotBierPck(fwd4, fwd6, fwdE, pck)) {
                    logger.info("received invalid bier protocol on label " + lab.label);
                }
            }
            return;
        }
        if (!pck.MPLSbottom) {
            cntrT.drop(pck, counter.reasons.badProto);
            return;
        }
        if (lab.pweIfc != null) {
            pck.getSkip(lab.pweDel);
            if (lab.pweAdd != null) {
                pck.putCopy(lab.pweAdd, 0, 0, lab.pweAdd.length);
                pck.putSkip(lab.pweAdd.length);
                pck.merge2beg();
            }
            lab.pweIfc.recvPack(pck);
            return;
        }
        if (ipCore.parseIPheader(pck, true)) {
            cntrT.drop(pck, counter.reasons.badHdr);
            return;
        }
        pck.INTiface = 0;
        pck.INTupper = -3;
        ipFwdIface ifc;
        if (pck.IPmlt || pck.IPbrd) {
            ifc = ipFwdTab.findStableIface(this);
        } else {
            ipFwd fwd = lab.forwarder;
            if (fwd == null) {
                cntrT.drop(pck, counter.reasons.notInTab);
                return;
            }
            tabRouteEntry<addrIP> prf = fwd.actualU.route(pck.IPtrg);
            if (prf == null) {
                cntrT.drop(pck, counter.reasons.noRoute);
                return;
            }
            if (prf.best.rouTab != null) {
                prf = prf.best.rouTab.actualU.route(prf.best.nextHop);
                if (prf == null) {
                    cntrT.drop(pck, counter.reasons.noRoute);
                    return;
                }
            }
            ifc = (ipFwdIface) prf.best.iface;
        }
        if (ifc == null) {
            cntrT.drop(pck, counter.reasons.noIface);
            return;
        }
        forwardPacket(3, ifc, null, pck);
    }

    /**
     * protocol signals that it sends a packet over mpls
     *
     * @param trg target address
     * @param pck packet to process
     * @param req require labeled path
     */
    public void mplsTxPack(addrIP trg, packHolder pck, boolean req) {
        pck.IPtrg.setAddr(trg);
        tabRouteEntry<addrIP> prf = actualU.route(trg);
        if (prf == null) {
            cntrT.drop(pck, counter.reasons.noRoute);
            return;
        }
        if ((prf.best.labelRem == null) && (req)) {
            if (prf.best.rouTyp == tabRouteAttr.routeType.conn) {
                doMpls((ipFwdIface) prf.best.iface, trg, null, pck);
                return;
            }
            logger.info("no label for " + trg);
            cntrT.drop(pck, counter.reasons.notInTab);
            return;
        }
        if (prf.best.rouTab != null) {
            ipMpls.createMPLSlabels(pck, prf.best.labelRem);
            prf.best.rouTab.mplsTxPack(prf.best.nextHop, pck, true);
            return;
        }
        if (prf.best.nextHop != null) {
            trg = prf.best.nextHop;
        }
        doMpls((ipFwdIface) prf.best.iface, trg, prf.best.labelRem, pck);
    }

    /**
     * forwards one parsed packet by policy routing
     *
     * @param cfg config
     * @param from source
     * @param rxIfc receiving interface
     * @param pck packet
     * @return true if sent, false if not
     */
    private boolean doPbrFwd(tabListing<tabPbrN, addrIP> cfg, int from, ipFwdIface rxIfc, packHolder pck) {
        if (cfg.size() < 1) {
            return false;
        }
        cfg.packParse(false, true, true, pck);
        tabPbrN pbr = cfg.find(pck);
        if (pbr == null) {
            return false;
        }
        if ((pbr.setSp > 0) && (pbr.setSi > 0)) {
            if (ipVersion == ipCor4.protocolVersion) {
                pck.IPprt = ifcNshFwd.protIp4;
            } else {
                pck.IPprt = ifcNshFwd.protIp6;
            }
            pck.NSHttl = 63;
            pck.NSHmdt = 2;
            pck.NSHmdv = new byte[0];
            pck.NSHsp = pbr.setSp;
            pck.NSHsi = pbr.setSi;
            ipMpls.gotNshPack(null, pck);
            return true;
        }
        if (pbr.setIfc != null) {
            pck.INTiface = -2;
            ifaceProto(pbr.setIfc, pck, pbr.setHop);
            return true;
        }
        if (pbr.setHop == null) {
            pck.INTiface = -2;
            pbr.setVrf.forwardPacket(from, rxIfc, null, pck);
            return true;
        }
        tabRouteEntry<addrIP> ntry = pbr.setVrf.actualU.route(pbr.setHop);
        if (ntry == null) {
            return false;
        }
        if (ntry.best.iface == null) {
            return false;
        }
        pck.INTiface = -2;
        ifaceProto((ipFwdIface) ntry.best.iface, pck, pbr.setHop);
        return true;
    }

    /**
     * forwards one parsed packet
     *
     * @param from source, 1=ifc, 2=mpls, 4=proto
     * @param rxIfc receiving interface
     * @param hop target hop
     * @param pck packet
     */
    private void forwardPacket(int from, ipFwdIface rxIfc, addrIP hop, packHolder pck) {
        cntrT.rx(pck);
        if (rxIfc == null) {
            cntrT.drop(pck, counter.reasons.noIface);
            return;
        }
        if (packetFilter != null) {
            if (!packetFilter.matches(false, true, pck)) {
                doDrop(pck, rxIfc, counter.reasons.denied);
                return;
            }
        }
        if (!rxIfc.disableDapp && (dapp != null)) {
            if (dapp.checkPacket(bits.getTime(), pck)) {
                cntrT.drop(pck, counter.reasons.noBuffer);
                return;
            }
        }
        if (!rxIfc.disableFlowspec && (flowspec != null)) {
            if (flowspec.checkPacket(bits.getTime(), pck)) {
                cntrT.drop(pck, counter.reasons.noBuffer);
                return;
            }
        }
        if (debugger.ipFwdTraf) {
            logger.debug("fwd " + pck.IPsrc + " -> " + pck.IPtrg + " pr=" + pck.IPprt + " tos=" + pck.IPtos);
        }
        if (netflow != null) {
            netflow.session.doPack(pck, true);
        }
        pck.ETHcos = (pck.IPtos >>> 5) & 7;
        pck.MPLSexp = pck.ETHcos;
        if (natCfg.size() > 0) {
            natCfg.packParse(false, true, true, pck);
            tabNatTraN natT = tabNatTraN.fromPack(pck);
            natT = natTrns.find(natT);
            if (natT != null) {
                long tim = bits.getTime();
                natT.lastUsed = tim;
                natT.reverse.lastUsed = tim;
                natT.updatePack(pck);
                natCfg.packUpdate(pck);
            } else {
                tabNatCfgN natC = natCfg.find(pck);
                if (natC != null) {
                    natT = natC.createEntry(pck, icmpCore);
                    natTrns.add(natT);
                    natTrns.add(natT.reverseEntry());
                    natT.updatePack(pck);
                    natCfg.packUpdate(pck);
                    tableChanger();
                }
            }
        }
        boolean alerted = (pck.IPalrt != -1);
        pck.IPalrt = -1;
        if (doPbrFwd(pbrCfg, from, rxIfc, pck)) {
            return;
        }
        if (hop != null) {
            ifaceProto(rxIfc, pck, hop);
            return;
        }
        if (pck.IPlnk) {
            if ((from & 1) != 0) {
                if (rxIfc.lower.checkMyAddress(pck.IPtrg)) {
                    protoSend(rxIfc, pck);
                    return;
                }
                if (rxIfc.lower.checkMyAlias(pck.IPtrg) != null) {
                    protoSend(rxIfc, pck);
                    return;
                }
                doDrop(pck, rxIfc, counter.reasons.noRoute);
            } else {
                ifaceProto(rxIfc, pck, null);
            }
            return;
        }
        if (pck.IPmlt || pck.IPbrd) {
            if (pck.IPbrd || !pck.IPmlr) {
                if ((from & 1) != 0) {
                    protoSend(rxIfc, pck);
                } else {
                    ifaceProto(rxIfc, pck, null);
                }
                return;
            }
            if (pck.IPttl < 2) {
                cntrT.drop(pck, counter.reasons.ttlExceed);
                return;
            }
            if (from != 4) {
                ipCore.updateIPheader(pck, null, null, -1, -2, -1, -1);
            }
            ipFwdMcast grp = new ipFwdMcast(pck.IPtrg, pck.IPsrc);
            grp = groups.find(grp);
            if (grp == null) {
                cntrT.drop(pck, counter.reasons.badNet);
                return;
            }
            if ((from & 3) == 1) {
                if (grp.iface == null) {
                    cntrT.drop(pck, counter.reasons.noRoute);
                    return;
                }
                if (grp.iface.ifwNum != rxIfc.ifwNum) {
                    cntrT.drop(pck, counter.reasons.noRoute);
                    return;
                }
            }
            for (int i = 0; i < grp.flood.size(); i++) {
                ipFwdIface ifc = grp.flood.get(i);
                if (ifc == null) {
                    continue;
                }
                if ((from & 3) == 1) {
                    if (grp.iface.ifwNum == ifc.ifwNum) {
                        continue;
                    }
                }
                if (pck.IPttl < ifc.mcastTtl) {
                    continue;
                }
                ifaceProto(ifc, pck.copyBytes(true, true), null);
            }
            if (grp.label != null) {
                grp.label.sendPack(this, pck);
            }
            if (grp.bier != null) {
                grp.bier.sendPack(pck);
            }
            if (grp.local && ((from & 1) != 0)) {
                protoSend(rxIfc, pck);
            }
            return;
        }
        tabRouteEntry<addrIP> prf = actualU.route(pck.IPtrg);
        if (prf == null) {
            doDrop(pck, rxIfc, counter.reasons.noRoute);
            return;
        }
        if (prf.cntr != null) {
            prf.cntr.tx(pck);
        }
        if (prf.best.rouTab != null) {
            cntrT.tx(pck);
            if (prf.best.segrouPrf != null) {
                pck.putDefaults();
                pck.IPtrg.setAddr(prf.best.segrouPrf);
                pck.IPsrc.setAddr(prf.best.segrouPrf);
                pck.IPprt = ipCore.getProtocol();
                prf.best.rouTab.createIPheader(pck);
                ipMpls.beginMPLSfields(pck, false);
                prf.best.rouTab.forwardPacket(from, rxIfc, null, pck);
                return;
            }
            if (prf.best.labelRem == null) {
                doDrop(pck, rxIfc, counter.reasons.notInTab);
                return;
            }
            ipMpls.createMPLSlabels(pck, prf.best.labelRem);
            prf.best.rouTab.mplsTxPack(prf.best.nextHop, pck, true);
            return;
        }
        if (prf.best.iface == null) {
            doDrop(pck, rxIfc, counter.reasons.noIface);
            return;
        }
        ipFwdIface txIfc = (ipFwdIface) prf.best.iface;
        if (txIfc.lower.checkMyAddress(pck.IPtrg)) {
            protoSend(txIfc, pck);
            return;
        }
        if (txIfc.lower.checkMyAlias(pck.IPtrg) != null) {
            protoSend(txIfc, pck);
            return;
        }
        if (pck.MPLSttl < 2) {
            doDrop(pck, rxIfc, counter.reasons.ttlExceed);
            return;
        }
        if (((from & 1) != 0) && alerted) {
            if (!protoAlert(rxIfc, pck)) {
                return;
            }
        }
        if (from != 4) {
            if ((mplsPropTtl | txIfc.mplsPropTtlAlways) & txIfc.mplsPropTtlAllow) {
                ipCore.updateIPheader(pck, null, null, -1, pck.MPLSttl - 1, -1, -1);
            } else {
                ipCore.updateIPheader(pck, null, null, -1, -2, -1, -1);
            }
        }
        if (prf.best.rouTyp == tabRouteAttr.routeType.conn) {
            ifaceProto(txIfc, pck, null);
            return;
        }
        if (alerted) {
            ifaceProto(txIfc, pck, prf.best.nextHop);
            return;
        }
        doMpls(txIfc, prf.best.nextHop, prf.best.labelRem, pck);
    }

    /**
     * got error report
     *
     * @param err error code
     * @param iface receiving interface
     * @param pck protocol packet
     */
    public void errorReport(counter.reasons err, ipFwdIface iface, packHolder pck) {
        addrIP rtr = pck.IPsrc.copyBytes();
        if (ipCore.parseIPheader(pck, false)) {
            iface.cntr.drop(pck, counter.reasons.badHdr);
            return;
        }
        natCfg.packParse(false, true, false, pck);
        tabNatTraN natT = tabNatTraN.fromError(pck);
        natT = natTrns.find(natT);
        if (natT != null) {
            long tim = bits.getTime();
            natT.lastUsed = tim;
            natT.reverse.lastUsed = tim;
            natT.updateError(pck);
            natCfg.packUpdate(pck);
            if (icmpCore.createError(pck, err, rtr, false)) {
                return;
            }
            ipCore.createIPheader(pck);
            pck.INTupper = -1;
            ipMpls.beginMPLSfields(pck, (mplsPropTtl | iface.mplsPropTtlAlways) & iface.mplsPropTtlAllow);
            forwardPacket(4, iface, null, pck);
            return;
        }
        if (debugger.ipFwdTraf) {
            logger.debug("err " + pck.IPsrc + " -> " + pck.IPtrg + " pr=" + pck.IPprt + " rtr=" + rtr + " reason=" + counter.reason2string(err));
        }
        ipFwdProto prt = null;
        if (prt == null) {
            prt = protos.get(iface.ifwNum, pck.IPtrg, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            prt = protos.get(0, pck.IPtrg, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            prt = protos.get(iface.ifwNum, null, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            prt = protos.get(0, null, pck.IPprt, pck.IPprt);
        }
        if (prt == null) {
            return;
        }
        pck.getSkip(pck.IPsiz);
        prt.upper.errorPack(err, rtr, iface, pck);
    }

    /**
     * send echo request
     *
     * @param src source address, null if nearest
     * @param trg target address
     * @param size size of payload
     * @param ttl ttl to use
     * @param tos tos to use
     * @param dat filler byte
     * @param mul multiple responses
     * @return notifier notified on reply
     */
    public ipFwdEcho echoSendReq(addrIP src, addrIP trg, int size, int ttl, int tos, int dat, boolean mul) {
        final int maxSize = 8192;
        final int minSize = 16;
        if (size < minSize) {
            size = minSize;
        }
        if (size > maxSize) {
            size = maxSize;
        }
        packHolder pck = new packHolder(true, true);
        pck.putFill(0, size, dat);
        pck.putSkip(size);
        pck.merge2beg();
        ipFwdEcho ntry = new ipFwdEcho();
        ntry.notif = new notifier();
        ipFwdIface ifc;
        if (src == null) {
            ifc = ipFwdTab.findSendingIface(this, trg);
            if (ifc == null) {
                return null;
            }
            src = ifc.addr.copyBytes();
        } else {
            ifc = ipFwdTab.findSendingIface(this, src);
            if (ifc == null) {
                return null;
            }
        }
        ntry.src = src.copyBytes();
        ntry.trg = trg.copyBytes();
        ntry.multi = mul;
        for (;;) {
            nextEchoNumber = (nextEchoNumber & 0x3fffffff) + 1;
            ntry.echoNum = nextEchoNumber + 10000;
            if (echoes.add(ntry) == null) {
                break;
            }
        }
        ntry.created = bits.getTime();
        if (icmpCore.createEcho(pck, src, trg, ntry.echoNum)) {
            return null;
        }
        pck.IPttl = ttl;
        pck.IPtos = tos;
        pck.INTupper = -1;
        ipCore.createIPheader(pck);
        if (coppOut != null) {
            if (coppOut.checkPacket(bits.getTime(), pck)) {
                cntrL.drop(pck, counter.reasons.noBuffer);
                return ntry;
            }
        }
        ipMpls.beginMPLSfields(pck, (mplsPropTtl | ifc.mplsPropTtlAlways) & ifc.mplsPropTtlAllow);
        forwardPacket(4, ifc, null, pck);
        return ntry;
    }

    /**
     * got echo reply packet
     *
     * @param pck packet received
     * @param id id received
     */
    public void echoRecvRep(packHolder pck, int id) {
        ipFwdEcho ntry = new ipFwdEcho();
        ntry.echoNum = id;
        ntry = echoes.find(ntry);
        if (ntry == null) {
            return;
        }
        if (ntry.src.compare(ntry.src, pck.IPtrg) != 0) {
            return;
        }
        if (!ntry.multi) {
            echoes.del(ntry);
        }
        ipFwdEchod res = new ipFwdEchod();
        res.tim = (int) (bits.getTime() - ntry.created);
        res.err = null;
        res.rtr = pck.IPsrc.copyBytes();
        res.lab = -1;
        ntry.res.add(res);
        ntry.notif.wakeup();
    }

    /**
     * got error report to ping
     *
     * @param pck packet received
     * @param id id received
     * @param err error reported
     * @param rtr reporting router
     */
    public void echoRecvErr(packHolder pck, int id, counter.reasons err, addrIP rtr) {
        ipFwdEcho ntry = new ipFwdEcho();
        ntry.echoNum = id;
        ntry = echoes.find(ntry);
        if (ntry == null) {
            return;
        }
        if (ntry.trg.compare(ntry.trg, pck.IPtrg) != 0) {
            return;
        }
        if (ntry.src.compare(ntry.src, pck.IPsrc) != 0) {
            return;
        }
        if (!ntry.multi) {
            echoes.del(ntry);
        }
        ipFwdEchod res = new ipFwdEchod();
        res.tim = (int) (bits.getTime() - ntry.created);
        res.err = err;
        res.rtr = rtr.copyBytes();
        res.lab = ipFwdEcho.getMplsExt(pck);
        ntry.res.add(res);
        ntry.notif.wakeup();
    }

    public void run() {
        try {
            if (debugger.ipFwdEvnt) {
                logger.debug("startup");
            }
            for (;;) {
                if (triggerUpdate.misleep(untriggeredRecomputation) > 0) {
                    if (debugger.ipFwdEvnt) {
                        logger.debug("too fast table updates");
                    }
                }
                if (untriggeredRecomputation <= 0) {
                    break;
                }
                if (debugger.ipFwdEvnt) {
                    logger.debug("update tables");
                }
                ipFwdTab.updateEverything(this);
            }
            untriggeredRecomputation -= 1;
            if (debugger.ipFwdEvnt) {
                logger.debug("shutdown");
            }
        } catch (Exception e) {
            logger.exception(e);
        }
    }

}
