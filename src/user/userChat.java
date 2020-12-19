package user;

import auth.authResult;
import cfg.cfgAll;
import java.util.ArrayList;
import java.util.List;
import pipe.pipeSetting;
import pipe.pipeSide;
import util.bits;

/**
 * user chat
 *
 * @author matecsaba
 */
public class userChat implements Runnable {

    private final pipeSide pipe;

    private final userReader read;

    private boolean need2run;

    /**
     * create new instance
     *
     * @param con pipeline to use
     * @param rdr reader to use
     */
    public userChat(pipeSide con, userReader rdr) {
        pipe = con;
        read = rdr;
    }

    /**
     * do the chat
     */
    public void doChat() {
        pipe.linePut("entering chat, type /exit to exit");
        need2run = true;
        new Thread(this).start();
        for (;;) {
            userHelping hl = new userHelping();
            hl.add("1 1,.   <text>                      chat line");
            read.setContext(hl, "you:");
            String a = read.readLine("/exit");
            if (a == null) {
                break;
            }
            if (a.trim().toLowerCase().equals("/exit")) {
                break;
            }
            cfgAll.chat.send(((authResult) pipe.settingsGet(pipeSetting.authed, new authResult())).user, a);
        }
        need2run = false;
    }

    public void run() {
        long old = 0;
        for (;;) {
            bits.sleep(100);
            if (!need2run) {
                break;
            }
            final int width = (int) pipe.settingsGet(pipeSetting.width, 80);
            final int height = (int) pipe.settingsGet(pipeSetting.height, 25);
            List<String> l = new ArrayList<String>();
            old = cfgAll.chat.read(l, old, height - 5);
            if (l.size() < 1) {
                continue;
            }
            for (int i = 0; i < l.size(); i++) {
                pipe.blockingPut(pipeSide.getEnding(pipeSide.modTyp.modeCR), 0, 1);
                pipe.linePut(bits.padEnd("" + l.get(i), width, " "));
            }
            read.putCurrLine(true);
        }
    }

}
