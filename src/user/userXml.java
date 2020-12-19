package user;

import java.util.ArrayList;
import java.util.List;
import pipe.pipeLine;
import pipe.pipeSetting;
import pipe.pipeSide;
import util.cmds;
import util.debugger;
import util.extMrkLng;
import util.extMrkLngEntry;
import util.logger;

/**
 * xml handler
 *
 * @author matecsaba
 */
public class userXml {

    private final pipeSide conn;

    private final boolean privi;

    private final boolean form;

    private final boolean echo;

    private static final String prompt = "XML> ";

    /**
     * create handler
     *
     * @param pipe pipe to use
     * @param prv privileged
     * @param frm format response
     * @param ech echo input
     */
    public userXml(pipeSide pipe, boolean prv, boolean frm, boolean ech) {
        conn = pipe;
        privi = prv;
        form = frm;
        echo = ech;
    }

    /**
     * do request
     *
     * @param req request
     * @return response, null if error
     */
    public extMrkLng doRequest(extMrkLng req) {
        extMrkLng rep = new extMrkLng();
        rep.data.add(new extMrkLngEntry(null, "/Response", "", ""));
        rep.data.add(new extMrkLngEntry(null, "/Response/CLI", "MajorVersion=\"1\" MinorVersion=\"0\"", ""));
        for (int i = 0; i < req.data.size(); i++) {
            extMrkLngEntry ntry = req.data.get(i);
            if (ntry.name.equals("/?xml/Request/CLI/Exec")) {
                cmds cmd = new cmds("xml", ntry.value);
                String e = new String(pipeSide.getEnding(pipeSide.modTyp.modeCRLF));
                for (;;) {
                    if (cmd.size() < 1) {
                        break;
                    }
                    String s = cmd.word("\n").trim();
                    if (s.length() < 1) {
                        continue;
                    }
                    pipeLine pl = new pipeLine(1024 * 1024, false);
                    pipeSide pip = pl.getSide();
                    pip.lineTx = pipeSide.modTyp.modeCRLF;
                    pip.lineRx = pipeSide.modTyp.modeCRorLF;
                    userReader rdr = new userReader(pip, null);
                    pip.settingsPut(pipeSetting.tabMod, userFormat.tableMode.raw);
                    pip.settingsPut(pipeSetting.height, 0);
                    userExec exe = new userExec(pip, rdr);
                    exe.privileged = privi;
                    pip.setTime(60000);
                    String a = exe.repairCommand(s);
                    String r = "#" + a + e;
                    exe.executeCommand(a);
                    pip = pl.getSide();
                    pl.setClose();
                    s = pip.strGet(1024 * 1024);
                    if (s == null) {
                        continue;
                    }
                    rep.data.add(new extMrkLngEntry(null, "/Response/CLI/Exec", "", r + s));
                }
                continue;
            }
            if (ntry.name.equals("/?xml/Request/CLI/Configuration")) {
                if (!privi) {
                    rep.data.add(new extMrkLngEntry(null, "/Response/CLI/Configuration", "", "not enough privileges"));
                    continue;
                }
                cmds cmd = new cmds("xml", ntry.value);
                pipeLine pl = new pipeLine(65535, false);
                pipeSide pip = pl.getSide();
                pip.lineTx = pipeSide.modTyp.modeCRLF;
                pip.lineRx = pipeSide.modTyp.modeCRorLF;
                userReader rdr = new userReader(pip, null);
                pip.settingsPut(pipeSetting.tabMod, userFormat.tableMode.raw);
                pip.settingsPut(pipeSetting.height, 0);
                userConfig cfg = new userConfig(pip, rdr);
                pip.setTime(60000);
                for (;;) {
                    if (cmd.size() < 1) {
                        break;
                    }
                    String s = cmd.word("\n").trim();
                    if (s.length() < 1) {
                        continue;
                    }
                    userHelping hlp = cfg.getHelping(true, true);
                    rdr.setContext(hlp, "");
                    String b = hlp.repairLine(s);
                    if (b.length() < 1) {
                        pip.linePut("bad: " + s);
                        continue;
                    }
                    pip.linePut("#" + b);
                    cfg.executeCommand(b);
                }
                pip = pl.getSide();
                pl.setClose();
                String a = pip.strGet(65535);
                if (a == null) {
                    a = "";
                }
                rep.data.add(new extMrkLngEntry(null, "/Response/CLI/Configuration", "", a));
                continue;
            }
        }
        rep.data.add(new extMrkLngEntry(null, "/Response/CLI", "", ""));
        rep.data.add(new extMrkLngEntry(null, "/Response", "", ""));
        return rep;
    }

    /**
     * read request
     *
     * @return request, null if error
     */
    public extMrkLng doRead() {
        List<String> l = new ArrayList<String>();
        for (;;) {
            if (conn.isClosed() != 0) {
                return null;
            }
            String a = conn.lineGet(echo ? 0x32 : 1);
            if (debugger.userXmlEvnt) {
                logger.debug("rx: " + a);
            }
            if (a.equals("<Exit/>")) {
                return null;
            }
            if (a.length() < 1) {
                continue;
            }
            l.add(a);
            if (a.indexOf("</Request>") >= 0) {
                break;
            }
        }
        extMrkLng x = new extMrkLng();
        if (x.fromString(l, "\n")) {
            return null;
        }
        return x;
    }

    /**
     * send response
     *
     * @param x xml
     */
    public void doSend(extMrkLng x) {
        if (debugger.userXmlEvnt) {
            logger.debug("tx: " + x.toXMLstr());
        }
        if (!form) {
            conn.linePut(extMrkLng.header + "\n" + x.toXMLstr());
            conn.strPut(prompt);
            return;
        }
        conn.linePut(extMrkLng.header);
        List<String> r = x.toXMLlst();
        for (int i = 0; i < r.size(); i++) {
            conn.linePut(r.get(i));
        }
        conn.strPut(prompt);
    }

    /**
     * do work
     */
    public void doWork() {
        conn.strPut(prompt);
        for (;;) {
            extMrkLng x = doRead();
            if (x == null) {
                break;
            }
            x = doRequest(x);
            if (x == null) {
                conn.linePut(extMrkLng.header + "\n<Response MajorVersion=\"1\" MinorVersion=\"0\" ErrorCode=\"1\" ErrorMsg=\"request error\"><ResultSummary ErrorCount=\"0\"/></Response>");
                conn.strPut(prompt);
                continue;
            }
            doSend(x);
        }
    }

}
