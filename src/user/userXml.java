package user;

import java.util.List;
import pipe.pipeSide;
import util.extMrkLng;
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
        return null;
    }

    /**
     * do work
     */
    public void doWork() {
        String s = "";
        conn.strPut(prompt);
        for (;;) {
            if (conn.isClosed() != 0) {
                return;
            }
            s += conn.lineGet(echo ? 0x32 : 1);
            if (s.equals("exit")) {
                return;
            }
            if (s.indexOf("<Request") < 0) {
                continue;
            }
            if (s.indexOf("</Request>") < 0) {
                continue;
            }
            extMrkLng x = new extMrkLng();
            boolean b = x.fromString(s);
            logger.debug("here " + s + " " + b);
            s = "";
            if (!b) {
                conn.linePut(extMrkLng.header + "\n<Response MajorVersion=\"1\" MinorVersion=\"0\" ErrorCode=\"1\" ErrorMsg=\"parse error\"><ResultSummary ErrorCount=\"0\"/></Response>");
                conn.strPut(prompt);
                continue;
            }
            x = doRequest(x);
            if (x == null) {
                conn.linePut(extMrkLng.header + "\n<Response MajorVersion=\"1\" MinorVersion=\"0\" ErrorCode=\"2\" ErrorMsg=\"request error\"><ResultSummary ErrorCount=\"0\"/></Response>");
                conn.strPut(prompt);
                continue;
            }
            if (!form) {
                conn.linePut(extMrkLng.header + "\n" + x.toXMLstr());
                conn.strPut(prompt);
                continue;
            }
            conn.linePut(extMrkLng.header);
            List<String> r = x.toXMLlst();
            for (int i = 0; i < r.size(); i++) {
                conn.linePut(r.get(i));
            }
            conn.strPut(prompt);
        }
    }
    
}
