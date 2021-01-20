package clnt;

import pipe.pipeProgress;
import pipe.pipeSide;
import serv.servCharGen;
import serv.servDiscard;
import serv.servGeneric;
import user.userTerminal;
import util.bits;
import util.cmds;

/**
 *
 * @author matecsaba
 */
public class clntSpeed {

    /**
     * rx pipeline
     */
    protected pipeSide rxp;

    /**
     * tx pipeline
     */
    protected pipeSide txp;

    /**
     * rx counter
     */
    protected int rxc;

    /**
     * tx counter
     */
    protected int txc;

    /**
     * small servers client
     *
     * @param cmd command to use
     */
    public static void smllClnt(cmds cmd) {
        userTerminal t = new userTerminal(new pipeProgress(cmd.pipe));
        String a = cmd.word();
        clntSpeed s = new clntSpeed();
        s.rxp = t.resolvAndConn(servGeneric.protoTcp, a, new servCharGen().srvPort(), "speed");
        if (s.rxp == null) {
            return;
        }
        s.txp = t.resolvAndConn(servGeneric.protoTcp, a, new servDiscard().srvPort(), "speed");
        if (s.txp == null) {
            s.rxp.setClose();
            return;
        }
        new Thread(new clntSpeedRx(s)).start();
        new Thread(new clntSpeedTx(s)).start();
        cmd.error("       rxbps       txbps");
        for (;;) {
            if (cmd.pipe.isClosed() != 0) {
                break;
            }
            if (cmd.pipe.ready2rx() != 0) {
                break;
            }
            int rxo = s.rxc;
            int txo = s.txc;
            bits.sleep(1000);
            cmd.error(bits.padBeg(bits.toUser((s.rxc - rxo) * 8), 12, " ") + bits.padBeg(bits.toUser((s.txc - txo) * 8), 12, " "));
        }
        s.rxp.setClose();
        s.txp.setClose();
    }

}

class clntSpeedRx implements Runnable {

    private clntSpeed parent;

    public clntSpeedRx(clntSpeed lower) {
        parent = lower;
    }

    public void run() {
        for (;;) {
            byte[] buf = new byte[1024];
            int i = parent.rxp.blockingGet(buf, 0, buf.length);
            if (i < 0) {
                break;
            }
            parent.rxc += i;
        }
    }

}

class clntSpeedTx implements Runnable {

    private clntSpeed parent;

    public clntSpeedTx(clntSpeed lower) {
        parent = lower;
    }

    public void run() {
        for (;;) {
            byte[] buf = new byte[1024];
            int i = parent.txp.blockingPut(buf, 0, buf.length);
            if (i < 0) {
                break;
            }
            parent.txc += i;
        }
    }

}
