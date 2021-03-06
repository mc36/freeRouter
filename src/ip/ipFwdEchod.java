package ip;

import addr.addrIP;
import util.counter;

/**
 * stores one echo reply
 *
 * @author matecsaba
 */
public class ipFwdEchod {

    /**
     * reporting router
     */
    public addrIP rtr;

    /**
     * reported error
     */
    public counter.reasons err;

    /**
     * reported label
     */
    public int lab;

    /**
     * time elapsed
     */
    public int tim;

}
