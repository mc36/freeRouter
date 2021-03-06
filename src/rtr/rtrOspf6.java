package rtr;

import addr.addrIP;
import addr.addrIPv4;
import addr.addrIPv6;
import addr.addrPrefix;
import cfg.cfgAll;
import cfg.cfgPrfxlst;
import cfg.cfgRoump;
import cfg.cfgRouplc;
import ip.ipFwd;
import ip.ipFwdIface;
import ip.ipRtr;
import java.util.ArrayList;
import java.util.List;
import pack.packHolder;
import tab.tabGen;
import tab.tabLabel;
import tab.tabLabelBier;
import tab.tabLabelNtry;
import tab.tabRoute;
import tab.tabRouteAttr;
import tab.tabRouteEntry;
import user.userFlash;
import user.userFormat;
import user.userHelping;
import util.bits;
import util.cmds;
import util.debugger;
import util.logger;
import util.shrtPthFrst;
import util.state;

/**
 * open shortest path first (rfc5340) protocol v3
 *
 * @author matecsaba
 */
public class rtrOspf6 extends ipRtr {

    /**
     * protocol number
     */
    public final static int protoNum = 89;

    /**
     * protocol version number
     */
    public final static int verNum = 3;

    /**
     * protocol header size
     */
    public final static int sizeHead = 16;

    /**
     * router id
     */
    public addrIPv4 routerID;

    /**
     * traffic engineering id
     */
    public addrIPv6 traffEngID;

    /**
     * segment routing maximum
     */
    public int segrouMax = 0;

    /**
     * segment routing base
     */
    public int segrouBase = 0;

    /**
     * bier length
     */
    public int bierLen = 0;

    /**
     * bier maximum
     */
    public int bierMax = 0;

    /**
     * external distance
     */
    public int distantExt;

    /**
     * intra-area distance
     */
    public int distantInt;

    /**
     * inter-area distance
     */
    public int distantSum;

    /**
     * forwarding core
     */
    public final ipFwd fwdCore;

    /**
     * list of interfaces
     */
    protected tabGen<rtrOspf6iface> ifaces;

    /**
     * list of areas
     */
    protected tabGen<rtrOspf6area> areas;

    /**
     * segment routing labels
     */
    protected tabLabelNtry[] segrouLab;

    /**
     * bier labels
     */
    protected tabLabelNtry[] bierLab;

    /**
     * create one ospf process
     *
     * @param forwarder the ip protocol
     * @param id process id
     */
    public rtrOspf6(ipFwd forwarder, int id) {
        fwdCore = forwarder;
        ifaces = new tabGen<rtrOspf6iface>();
        areas = new tabGen<rtrOspf6area>();
        routerID = new addrIPv4();
        traffEngID = new addrIPv6();
        distantExt = 110;
        distantInt = 110;
        distantSum = 110;
        routerCreateComputed();
        fwdCore.routerAdd(this, tabRouteAttr.routeType.ospf6, id);
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        return "ospf on " + fwdCore;
    }

    /**
     * create computed
     */
    public synchronized void routerCreateComputed() {
        if (debugger.rtrOspf6evnt) {
            logger.debug("create table");
        }
        tabRoute<addrIP> tab = new tabRoute<addrIP>("ospf");
        for (int i = 0; i < areas.size(); i++) {
            rtrOspf6area ntry = areas.get(i);
            if (ntry == null) {
                continue;
            }
            tab.mergeFrom(tabRoute.addType.ecmp, ntry.routes, null, true, tabRouteAttr.distanLim);
        }
        if (segrouLab != null) {
            for (int o = 0; o < segrouLab.length; o++) {
                boolean b = false;
                for (int i = 0; i < areas.size(); i++) {
                    rtrOspf6area ntry = areas.get(i);
                    if (ntry == null) {
                        continue;
                    }
                    if (ntry.segrouUsd == null) {
                        continue;
                    }
                    b |= ntry.segrouUsd[o];
                }
                if (!b) {
                    segrouLab[o].setFwdDrop(9);
                }
            }
        }
        if (bierLab != null) {
            int o = 0;
            for (int i = 0; i < ifaces.size(); i++) {
                rtrOspf6iface ifc = ifaces.get(i);
                if (ifc == null) {
                    continue;
                }
                if (ifc.brIndex < 1) {
                    continue;
                }
                o = ifc.brIndex;
                break;
            }
            tabLabelBier res = new tabLabelBier(bierLab[0].label, tabLabelBier.num2bsl(bierLen));
            res.idx = o;
            for (int i = 0; i < areas.size(); i++) {
                rtrOspf6area ntry = areas.get(i);
                if (ntry == null) {
                    continue;
                }
                res.mergeFrom(ntry.bierRes);
            }
            for (int i = 0; i < bierLab.length; i++) {
                bierLab[i].setBierMpls(21, fwdCore, res);
            }
        }
        tab.setProto(routerProtoTyp, routerProcNum);
        tab.preserveTime(routerComputedU);
        routerComputedU = tab;
        routerComputedM = tab;
        fwdCore.routerChg(this);
    }

    /**
     * redistribution changed
     */
    public void routerRedistChanged() {
        genLsas(3);
    }

    /**
     * others changed
     */
    public void routerOthersChanged() {
    }

    /**
     * get help
     *
     * @param l list
     */
    public void routerGetHelp(userHelping l) {
        l.add("1 2   router-id                   specify router id");
        l.add("2 .     <addr>                    router id");
        l.add("1 2   traffeng-id                 specify traffic engineering id");
        l.add("2 .     <addr>                    te id");
        l.add("1 2   segrout                     segment routing parameters");
        l.add("2 3,.   <num>                     maximum index");
        l.add("3 4       base                    specify base");
        l.add("4 3,.       <num>                 label base");
        l.add("1 2   bier                        bier parameters");
        l.add("2 3     <num>                     bitstring length");
        l.add("3 .       <num>                   maximum index");
        l.add("1 2   area                        configure one area");
        l.add("2 3     <num>                     area number");
        l.add("3 .       enable                  create this area");
        l.add("3 .       spf-bidir               spf bidir check");
        l.add("3 .       spf-topolog             spf topology logging");
        l.add("3 .       spf-hops                spf hops disallow");
        l.add("3 .       spf-ecmp                spf ecmp allow");
        l.add("3 4       spf-log                 spf log size");
        l.add("4 .         <num>                 number of entries");
        l.add("3 .       stub                    configure as stub");
        l.add("3 .       nssa                    configure as nssa");
        l.add("3 .       traffeng                configure for traffic engineering");
        l.add("3 .       segrout                 configure for segment routing");
        l.add("3 .       bier                    configure for bier");
        l.add("3 .       hostname                advertise hostname");
        l.add("3 .       default-originate       advertise default route");
        l.add("3 4       route-map-from          process prefixes from this area");
        l.add("4 .         <name>                name of route map");
        l.add("3 4       route-map-into          process prefixes into this area");
        l.add("4 .         <name>                name of route map");
        l.add("3 4       route-policy-from       process prefixes from this area");
        l.add("4 .         <name>                name of route map");
        l.add("3 4       route-policy-into       process prefixes into this area");
        l.add("4 .         <name>                name of route map");
        l.add("3 4       prefix-list-from        filter prefixes from this area");
        l.add("4 .         <name>                name of prefix list");
        l.add("3 4       prefix-list-into        filter prefixes into this area");
        l.add("4 .         <name>                name of prefix list");
        l.add("1 2   distance                    specify default distance");
        l.add("2 3     <num>                     intra-area distance");
        l.add("3 4       <num>                   inter-area distance");
        l.add("4 .         <num>                 external distance");
    }

    /**
     * get config
     *
     * @param l list
     * @param beg beginning
     * @param filter filter
     */
    public void routerGetConfig(List<String> l, String beg, int filter) {
        l.add(beg + "router-id " + routerID);
        l.add(beg + "traffeng-id " + traffEngID);
        String a = "";
        if (segrouBase != 0) {
            a += " base " + segrouBase;
        }
        cmds.cfgLine(l, segrouMax < 1, beg, "segrout", "" + segrouMax + a);
        cmds.cfgLine(l, bierMax < 1, beg, "bier", bierLen + " " + bierMax);
        for (int i = 0; i < areas.size(); i++) {
            rtrOspf6area ntry = areas.get(i);
            String s = "area " + ntry.area + " ";
            l.add(beg + s + "enable");
            l.add(beg + s + "spf-log " + ntry.lastSpf.logSize);
            cmds.cfgLine(l, ntry.lastSpf.topoLog.get() == 0, beg, s + "spf-topolog", "");
            cmds.cfgLine(l, ntry.lastSpf.bidir.get() == 0, beg, s + "spf-bidir", "");
            cmds.cfgLine(l, ntry.lastSpf.hops.get() == 0, beg, s + "spf-hops", "");
            cmds.cfgLine(l, ntry.lastSpf.ecmp.get() == 0, beg, s + "spf-ecmp", "");
            cmds.cfgLine(l, !ntry.stub, beg, s + "stub", "");
            cmds.cfgLine(l, !ntry.nssa, beg, s + "nssa", "");
            cmds.cfgLine(l, !ntry.traffEng, beg, s + "traffeng", "");
            cmds.cfgLine(l, !ntry.segrouEna, beg, s + "segrout", "");
            cmds.cfgLine(l, !ntry.bierEna, beg, s + "bier", "");
            cmds.cfgLine(l, !ntry.hostname, beg, s + "hostname", "");
            cmds.cfgLine(l, !ntry.defOrigin, beg, s + "default-originate", "");
            cmds.cfgLine(l, ntry.prflstFrom == null, beg, s + "prefix-list-from", "" + ntry.prflstFrom);
            cmds.cfgLine(l, ntry.prflstInto == null, beg, s + "prefix-list-into", "" + ntry.prflstInto);
            cmds.cfgLine(l, ntry.roumapFrom == null, beg, s + "route-map-from", "" + ntry.roumapFrom);
            cmds.cfgLine(l, ntry.roumapInto == null, beg, s + "route-map-into", "" + ntry.roumapInto);
            cmds.cfgLine(l, ntry.roupolFrom == null, beg, s + "route-policy-from", "" + ntry.roupolFrom);
            cmds.cfgLine(l, ntry.roupolInto == null, beg, s + "route-policy-into", "" + ntry.roupolInto);
        }
        l.add(beg + "distance " + distantInt + " " + distantSum + " " + distantExt);
    }

    /**
     * configure
     *
     * @param cmd command
     * @return false if success, true if error
     */
    public boolean routerConfigure(cmds cmd) {
        String s = cmd.word();
        if (s.equals("router-id")) {
            routerID.fromString(cmd.word());
            genLsas(3);
            return false;
        }
        if (s.equals("traffeng-id")) {
            traffEngID.fromString(cmd.word());
            genLsas(3);
            return false;
        }
        if (s.equals("segrout")) {
            tabLabel.release(segrouLab, 9);
            segrouMax = bits.str2num(cmd.word());
            segrouBase = 0;
            for (;;) {
                s = cmd.word();
                if (s.length() < 1) {
                    break;
                }
                if (s.equals("base")) {
                    segrouBase = bits.str2num(cmd.word());
                    continue;
                }
            }
            segrouLab = tabLabel.allocate(9, segrouBase, segrouMax);
            genLsas(3);
            return false;
        }
        if (s.equals("bier")) {
            tabLabel.release(bierLab, 21);
            bierLen = tabLabelBier.normalizeBsl(bits.str2num(cmd.word()));
            bierMax = bits.str2num(cmd.word());
            bierLab = tabLabel.allocate(21, (bierMax + bierLen - 1) / bierLen);
            genLsas(3);
            return false;
        }
        if (s.equals("distance")) {
            distantInt = bits.str2num(cmd.word());
            distantSum = bits.str2num(cmd.word());
            distantExt = bits.str2num(cmd.word());
            return false;
        }
        if (s.equals("area")) {
            rtrOspf6area dat = new rtrOspf6area(this, bits.str2num(cmd.word()));
            s = cmd.word();
            if (s.equals("enable")) {
                rtrOspf6area old = areas.add(dat);
                if (old != null) {
                    cmd.error("area already exists");
                    return false;
                }
                dat.startNow();
                dat.schedWork(7);
                return false;
            }
            dat = areas.find(dat);
            if (dat == null) {
                cmd.error("area not exists");
                return false;
            }
            if (s.equals("spf-log")) {
                dat.lastSpf.logSize.set(bits.str2num(cmd.word()));
                return false;
            }
            if (s.equals("spf-topolog")) {
                dat.lastSpf.topoLog.set(1);
                return false;
            }
            if (s.equals("spf-bidir")) {
                dat.lastSpf.bidir.set(1);
                dat.schedWork(3);
                return false;
            }
            if (s.equals("spf-hops")) {
                dat.lastSpf.hops.set(1);
                dat.schedWork(3);
                return false;
            }
            if (s.equals("spf-ecmp")) {
                dat.lastSpf.ecmp.set(1);
                dat.schedWork(3);
                return false;
            }
            if (s.equals("stub")) {
                dat.stub = true;
                dat.nssa = false;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("nssa")) {
                dat.stub = false;
                dat.nssa = true;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("traffeng")) {
                dat.traffEng = true;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("segrout")) {
                dat.segrouEna = true;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("bier")) {
                dat.bierEna = true;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("hostname")) {
                dat.hostname = true;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("default-originate")) {
                dat.defOrigin = true;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("prefix-list-from")) {
                cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
                if (ntry == null) {
                    cmd.error("no such prefix list");
                    return false;
                }
                dat.prflstFrom = ntry.prflst;
                dat.schedWork(7);
                return false;
            }
            if (s.equals("prefix-list-into")) {
                cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
                if (ntry == null) {
                    cmd.error("no such prefix list");
                    return false;
                }
                dat.prflstInto = ntry.prflst;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("route-map-from")) {
                cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
                if (ntry == null) {
                    cmd.error("no such route map");
                    return false;
                }
                dat.roumapFrom = ntry.roumap;
                dat.schedWork(7);
                return false;
            }
            if (s.equals("route-map-into")) {
                cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
                if (ntry == null) {
                    cmd.error("no such route map");
                    return false;
                }
                dat.roumapInto = ntry.roumap;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("route-policy-from")) {
                cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
                if (ntry == null) {
                    cmd.error("no such route policy");
                    return false;
                }
                dat.roupolFrom = ntry.rouplc;
                dat.schedWork(7);
                return false;
            }
            if (s.equals("route-policy-into")) {
                cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
                if (ntry == null) {
                    cmd.error("no such route policy");
                    return false;
                }
                dat.roupolInto = ntry.rouplc;
                dat.schedWork(3);
                return false;
            }
            return false;
        }
        if (!s.equals("no")) {
            return true;
        }
        s = cmd.word();
        if (s.equals("segrout")) {
            tabLabel.release(segrouLab, 9);
            segrouLab = null;
            segrouMax = 0;
            segrouBase = 0;
            genLsas(3);
            return false;
        }
        if (s.equals("bier")) {
            tabLabel.release(bierLab, 21);
            bierLab = null;
            bierLen = 0;
            bierMax = 0;
            genLsas(3);
            return false;
        }
        if (s.equals("area")) {
            rtrOspf6area dat = new rtrOspf6area(this, bits.str2num(cmd.word()));
            dat = areas.find(dat);
            if (dat == null) {
                cmd.error("area not exists");
                return false;
            }
            s = cmd.word();
            if (s.equals("enable")) {
                dat.stopNow();
                areas.del(dat);
                genLsas(3);
                return false;
            }
            if (s.equals("spf-log")) {
                dat.lastSpf.logSize.set(0);
                return false;
            }
            if (s.equals("spf-topolog")) {
                dat.lastSpf.topoLog.set(0);
                return false;
            }
            if (s.equals("spf-bidir")) {
                dat.lastSpf.bidir.set(0);
                dat.schedWork(3);
                return false;
            }
            if (s.equals("spf-hops")) {
                dat.lastSpf.hops.set(0);
                dat.schedWork(3);
                return false;
            }
            if (s.equals("spf-ecmp")) {
                dat.lastSpf.ecmp.set(0);
                dat.schedWork(3);
                return false;
            }
            if (s.equals("stub")) {
                dat.stub = false;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("nssa")) {
                dat.nssa = false;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("traffeng")) {
                dat.traffEng = false;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("segrout")) {
                dat.segrouEna = false;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("bier")) {
                dat.bierEna = false;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("hostname")) {
                dat.hostname = false;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("default-originate")) {
                dat.defOrigin = false;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("prefix-list-from")) {
                dat.prflstFrom = null;
                dat.schedWork(7);
                return false;
            }
            if (s.equals("prefix-list-into")) {
                dat.prflstInto = null;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("route-map-from")) {
                dat.roumapFrom = null;
                dat.schedWork(7);
                return false;
            }
            if (s.equals("route-map-into")) {
                dat.roumapInto = null;
                dat.schedWork(3);
                return false;
            }
            if (s.equals("route-policy-from")) {
                dat.roupolFrom = null;
                dat.schedWork(7);
                return false;
            }
            if (s.equals("route-policy-into")) {
                dat.roupolInto = null;
                dat.schedWork(3);
                return false;
            }
            return false;
        }
        return true;
    }

    /**
     * stop work
     */
    public void routerCloseNow() {
        for (int i = 0; i < areas.size(); i++) {
            rtrOspf6area ntry = areas.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.stopNow();
        }
        for (int i = 0; i < ifaces.size(); i++) {
            rtrOspf6iface ntry = ifaces.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.restartTimer(true);
            ntry.unregister2ip();
            ntry.closeNeighbors(true);
        }
        tabLabel.release(segrouLab, 9);
        tabLabel.release(bierLab, 21);
    }

    /**
     * generate lsas in all areas
     *
     * @param todo todo to pass
     */
    protected void genLsas(int todo) {
        todo &= 3;
        for (int i = 0; i < areas.size(); i++) {
            rtrOspf6area ntry = areas.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.schedWork(todo);
        }
    }

    /**
     * add ospf interface
     *
     * @param iface forwarding interface
     * @return interface handler
     */
    public rtrOspf6iface addInterface(ipFwdIface iface) {
        if (iface == null) {
            return null;
        }
        rtrOspf6area ara = areas.get(0);
        if (ara == null) {
            return null;
        }
        rtrOspf6iface ifc = new rtrOspf6iface(this, ara, iface);
        rtrOspf6iface old = ifaces.add(ifc);
        if (old != null) {
            return old;
        }
        ifc.register2ip();
        ifc.restartTimer(false);
        ara.schedWork(7);
        return ifc;
    }

    /**
     * delete ospf interface
     *
     * @param iface forwarding interface
     */
    public void delInterface(ipFwdIface iface) {
        rtrOspf6iface ifc = new rtrOspf6iface(this, null, iface);
        ifc = ifaces.del(ifc);
        if (ifc == null) {
            return;
        }
        ifc.closeUp(ifc.iface);
        ifc.schedWork(7);
    }

    /**
     * check if i am area border
     *
     * @return true if yes, false if no
     */
    protected boolean amIabr() {
        return areas.size() > 1;
    }

    /**
     * list neighbors
     *
     * @return list of neighbors
     */
    public userFormat showNeighs() {
        userFormat l = new userFormat("|", "interface|area|address|routerid|state|uptime");
        for (int o = 0; o < ifaces.size(); o++) {
            rtrOspf6iface ifc = ifaces.get(o);
            if (ifc == null) {
                continue;
            }
            for (int i = 0; i < ifc.neighs.size(); i++) {
                rtrOspf6neigh nei = ifc.neighs.get(i);
                if (nei == null) {
                    continue;
                }
                l.add(ifc + "|" + nei.area.area + "|" + nei.peer + "|" + nei.rtrID + "|" + nei.state + "|" + bits.timePast(nei.upTime));
            }
        }
        return l;
    }

    /**
     * list interfaces
     *
     * @return list of interfaces
     */
    public userFormat showIfaces() {
        userFormat l = new userFormat("|", "interface|neighbors");
        for (int i = 0; i < ifaces.size(); i++) {
            rtrOspf6iface ifc = ifaces.get(i);
            l.add(ifc.iface + "|" + ifc.neighs.size());
        }
        return l;
    }

    /**
     * list database
     *
     * @param area area number
     * @param cmd entry to find
     * @return list of entry
     */
    public List<String> showDatabase(int area, cmds cmd) {
        List<String> l = new ArrayList<String>();
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return l;
        }
        addrIPv4 ned1 = new addrIPv4();
        ned1.fromString(cmd.word());
        int ned2 = bits.str2num(cmd.word());
        for (int i = 0; i < ara.lsas.size(); i++) {
            rtrOspf6lsa ntry = ara.lsas.get(i);
            if (ntry == null) {
                continue;
            }
            if (ned2 != ntry.lsaID) {
                continue;
            }
            if (ned1.compare(ned1, ntry.rtrID) != 0) {
                continue;
            }
            l.add("" + ntry);
            packHolder pck = new packHolder(true, true);
            pck.putSkip(ntry.writeData(pck, 0, true));
            pck.merge2beg();
            userFlash.buf2hex(l, pck.getCopy(), 0);
            rtrOspfDump.dump6lsa(l, pck, ntry);
        }
        return l;
    }

    /**
     * list database
     *
     * @param area area number
     * @return list of database
     */
    public userFormat showDatabase(int area) {
        userFormat l = new userFormat("|", "routerid|lsaid|sequence|type|len|time");
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return l;
        }
        for (int i = 0; i < ara.lsas.size(); i++) {
            rtrOspf6lsa ntry = ara.lsas.get(i);
            if (ntry == null) {
                continue;
            }
            l.add("" + ntry);
        }
        return l;
    }

    /**
     * list routes
     *
     * @param area area number
     * @return list of routes
     */
    public tabRoute<addrIP> showRoute(int area) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return new tabRoute<addrIP>("empty");
        }
        return ara.routes;
    }

    /**
     * show spf
     *
     * @param area area number
     * @return log of spf
     */
    public userFormat showSpfStat(int area) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return null;
        }
        return ara.lastSpf.listStatistics();
    }

    /**
     * show spf
     *
     * @param area area number
     * @param cmd entry to find
     * @return log of spf
     */
    public userFormat showSpfTopo(int area, cmds cmd) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return null;
        }
        if (cmd.size() < 1) {
            return ara.lastSpf.listTopology();
        }
        rtrOspf6areaSpf ned = new rtrOspf6areaSpf(new addrIPv4(), 0);
        ned.fromString(cmd.word());
        return ara.lastSpf.listTopology(ned);
    }

    /**
     * show log
     *
     * @param area area number
     * @return log of spf
     */
    public userFormat showSpfLog(int area) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return null;
        }
        return ara.lastSpf.listUsages();
    }

    /**
     * show tree
     *
     * @param area area number
     * @return tree of spf
     */
    public List<String> showSpfTree(int area) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return new ArrayList<String>();
        }
        return ara.lastSpf.listTree();
    }

    /**
     * show tree
     *
     * @param area area number
     * @param cmd entry to find
     * @return tree of spf
     */
    public List<String> showSpfOtherTree(int area, cmds cmd) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return new ArrayList<String>();
        }
        shrtPthFrst<rtrOspf6areaSpf> spf = ara.lastSpf.copyBytes();
        rtrOspf6areaSpf ned = new rtrOspf6areaSpf(new addrIPv4(), 0);
        ned.fromString(cmd.word());
        spf.doCalc(ned, null);
        return spf.listTree();
    }

    /**
     * show topology
     *
     * @param area area number
     * @param cmd entry to find
     * @return log of spf
     */
    public userFormat showSpfOtherTopo(int area, cmds cmd) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return null;
        }
        shrtPthFrst<rtrOspf6areaSpf> spf = ara.lastSpf.copyBytes();
        rtrOspf6areaSpf ned = new rtrOspf6areaSpf(new addrIPv4(), 0);
        ned.fromString(cmd.word());
        spf.doCalc(ned, null);
        if (cmd.size() < 1) {
            return spf.listTopology();
        }
        ned = new rtrOspf6areaSpf(new addrIPv4(), 0);
        ned.fromString(cmd.word());
        return spf.listTopology(ned);
    }

    /**
     * show graph
     *
     * @param area area number
     * @return graph of spf
     */
    public List<String> showSpfGraph(int area) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return new ArrayList<String>();
        }
        return ara.lastSpf.listGraphviz();
    }

    /**
     * find neighbor
     *
     * @param area area
     * @param adr address
     * @return neighbor, null if not found
     */
    public rtrOspf6neigh findPeer(int area, addrIP adr) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return null;
        }
        for (int i = 0; i < ifaces.size(); i++) {
            rtrOspf6iface ifc = ifaces.get(i);
            rtrOspf6neigh nei = new rtrOspf6neigh(this, ara, ifc, adr.toIPv6());
            nei = ifc.neighs.find(nei);
            if (nei != null) {
                return nei;
            }
        }
        return null;
    }

    /**
     * get neighbor count
     *
     * @return count
     */
    public int routerNeighCount() {
        int o = 0;
        for (int i = 0; i < ifaces.size(); i++) {
            o += ifaces.get(i).neighs.size();
        }
        return o;
    }

    /**
     * list neighbors
     *
     * @param tab list
     */
    public void routerNeighList(tabRoute<addrIP> tab) {
        for (int o = 0; o < ifaces.size(); o++) {
            rtrOspf6iface ifc = ifaces.get(o);
            if (ifc == null) {
                continue;
            }
            if (ifc.iface.lower.getState() != state.states.up) {
                continue;
            }
            for (int i = 0; i < ifc.neighs.size(); i++) {
                rtrOspf6neigh nei = ifc.neighs.get(i);
                if (nei == null) {
                    continue;
                }
                addrIP adr = new addrIP();
                adr.fromIPv6addr(nei.peer);
                tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
                ntry.prefix = new addrPrefix<addrIP>(adr, addrIP.size * 8);
                tabRoute.addUpdatedEntry(tabRoute.addType.better, tab, rtrBgpUtil.sfiUnicast, 0, ntry, true, null, null, routerAutoMesh);
            }
        }
    }

    /**
     * get interface count
     *
     * @return count
     */
    public int routerIfaceCount() {
        return ifaces.size();
    }

    /**
     * get list of link states
     *
     * @param tab table to update
     * @param area area number
     * @param asn asn
     * @param adv advertiser
     */
    public void routerLinkStates(tabRoute<addrIP> tab, int area, int asn, addrIPv4 adv) {
        rtrOspf6area ara = new rtrOspf6area(this, area);
        ara = areas.find(ara);
        if (ara == null) {
            return;
        }
        ara.lastSpf.listLinkStates(tab, 6, ara.area, asn, adv, addrIPv4.size);
    }

}
