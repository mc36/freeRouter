package user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import pipe.pipeSetting;
import pipe.pipeSide;
import util.bits;
import util.cmds;
import util.debugger;
import util.logger;

/**
 * reading one line from the user
 *
 * @author matecsaba
 */
public class userReader implements Comparator<String> {

    private pipeSide pipe; // pipe to use

    private String prompt; // current prompt

    private userHelping help; // help context

    private String[] histD; // history data

    private int histN; // history number

    private String curr; // current line

    private String clip; // clipboard

    private int pos; // position of next typed character goes

    private int len; // length of current line

    private int beg; // first character displayed

    private boolean clear; // clearing of display required

    private String filterS; // filter string

    private mode filterM; // filter mode

    private mode filterF; // filter final mode

    private int columnB; // beginning of column

    private int columnE; // ending of column

    /**
     * operation modes
     */
    public enum mode {

        /**
         * no filtering
         */
        raw,
        /**
         * begin
         */
        begin,
        /**
         * end
         */
        end,
        /**
         * include
         */
        include,
        /**
         * exclude
         */
        exclude,
        /**
         * section names
         */
        headers,
        /**
         * first lines
         */
        first,
        /**
         * last lines
         */
        last,
        /**
         * count entities
         */
        count,
        /**
         * sort entities
         */
        sort,
        /**
         * unique entities
         */
        uniq,
        /**
         * redirection
         */
        redirect,
        /**
         * text viewer
         */
        viewer,
        /**
         * level hierarchy
         */
        level,
        /**
         * csv list
         */
        csv,
        /**
         * htmlized
         */
        html,
        /**
         * prepend line numbers
         */
        linenum,
        /**
         * specified section
         */
        section,
        /**
         * set/delete mode
         */
        setdel,
        /**
         * summary
         */
        summary

    }

    /**
     * constructs new reader for a pipeline
     *
     * @param pip pipeline to use as input
     * @param parent line to use
     */
    public userReader(pipeSide pip, userLine parent) {
        pipe = pip;
        setHistory(64);
        clip = "";
        filterS = "";
        if (parent == null) {
            pipe.settingsAdd(pipeSetting.spacTab, false);
            pipe.settingsAdd(pipeSetting.logging, false);
            pipe.settingsAdd(pipeSetting.times, false);
            pipe.settingsAdd(pipeSetting.colors, false);
            pipe.settingsAdd(pipeSetting.width, 79);
            pipe.settingsAdd(pipeSetting.height, 24);
            pipe.settingsAdd(pipeSetting.tabMod, userFormat.tableMode.normal);
            pipe.settingsAdd(pipeSetting.deactive, 65536);
            pipe.settingsAdd(pipeSetting.escape, 65536);
            return;
        }
        pipe.settingsAdd(pipeSetting.spacTab, parent.execSpace);
        pipe.settingsAdd(pipeSetting.logging, parent.execLogging);
        pipe.settingsAdd(pipeSetting.times, parent.execTimes);
        pipe.settingsAdd(pipeSetting.colors, parent.execColor);
        pipe.settingsAdd(pipeSetting.width, parent.execWidth);
        pipe.settingsAdd(pipeSetting.height, parent.execHeight);
        pipe.settingsAdd(pipeSetting.tabMod, parent.execTables);
        pipe.settingsAdd(pipeSetting.deactive, parent.promptDeActive);
        pipe.settingsAdd(pipeSetting.escape, parent.promptEscape);
    }

    /**
     * set width of screen
     *
     * @param pip pipe to set
     * @param siz size
     */
    public static void setTermWdt(pipeSide pip, int siz) {
        if (siz < 1) {
            return;
        }
        if (siz < 20) {
            siz = 20;
        }
        pip.settingsPut(pipeSetting.width, siz);
    }

    /**
     * set height of screen
     *
     * @param pip pipe to set
     * @param siz size
     */
    public static void setTermLen(pipeSide pip, int siz) {
        if (siz < 0) {
            siz = 0;
        }
        pip.settingsPut(pipeSetting.height, siz);
    }

    /**
     * set context in which input will be processed
     *
     * @param hlp helper instance
     * @param prmt promtp to show
     */
    public void setContext(userHelping hlp, String prmt) {
        help = hlp;
        prompt = prmt;
    }

    /**
     * set size of history buffer
     *
     * @param num size
     */
    public void setHistory(int num) {
        String[] d = new String[num];
        for (int i = 0; i < num; i++) {
            d[i] = "";
        }
        if (histD != null) {
            if (num > histD.length) {
                num = histD.length;
            }
            for (int i = 0; i < num; i++) {
                d[i] = histD[i];
            }
        }
        histD = d;
    }

    private int doMorePrompt() {
        for (;;) {
            pipe.strPut("\r  -=[more]=-\r");
            byte[] buf = new byte[1];
            if (pipe.blockingGet(buf, 0, buf.length) != buf.length) {
                return 3;
            }
            pipe.strPut("\r                   \r");
            switch (buf[0]) {
                case 13: // enter
                    return 2;
                case 32: // space
                    return 1;
                case 27: // escape
                case 3: // ctrl+c
                case 113: // q
                case 81: // Q
                    return 3;
            }
        }
    }

    private void findColumn(String s) {
        columnB = s.indexOf(filterS);
        columnE = columnB + filterS.length();
        if (columnB < 0) {
            return;
        }
        for (; columnE < s.length(); columnE++) {
            String a = s.substring(columnB, columnE).trim();
            if (!a.equals(filterS)) {
                break;
            }
        }
        columnE--;
    }

    public int compare(String o1, String o2) {
        return o1.substring(columnB, columnE).compareTo(o2.substring(columnB, columnE));
    }

    private List<String> doCount(List<String> lst) {
        int wrd = 0;
        int chr = 0;
        for (int i = 0; i < lst.size(); i++) {
            String a = lst.get(i).trim();
            for (;;) {
                String s = a.replaceAll("  ", " ");
                if (s.equals(a)) {
                    break;
                }
                a = s;
            }
            int o = a.length();
            a = a.replaceAll(" ", "");
            int p = a.length();
            wrd += o - p;
            if (p > 0) {
                wrd++;
            }
            chr += o;
        }
        final userFormat.tableMode tabMod = pipe.settingsGet(pipeSetting.tabMod, userFormat.tableMode.normal);
        if (tabMod == userFormat.tableMode.normal) {
            return bits.str2lst(lst.size() + " lines, " + wrd + " words, " + chr + " characters");
        }
        userFormat res = new userFormat("|", "category|value");
        res.add("lines|" + lst.size());
        res.add("words|" + wrd);
        res.add("chars|" + chr);
        return res.formatAll(tabMod);
    }

    private List<String> doSummary(List<String> lst) {
        List<Long> sum = new ArrayList<Long>();
        final userFormat.tableMode tabMod = pipe.settingsGet(pipeSetting.tabMod, userFormat.tableMode.normal);
        for (int i = 0; i < lst.size(); i++) {
            String a = lst.get(i).trim();
            switch (tabMod) {
                case raw:
                case csv:
                    a = a.replaceAll(";", " ").trim();
                    break;
                case fancy:
                case table:
                    a = a.replaceAll("\\|", " ").trim();
                    break;
            }
            cmds cmd = new cmds("row", a);
            for (int p = 0;; p++) {
                a = cmd.word();
                if (a.length() < 1) {
                    break;
                }
                if (p >= sum.size()) {
                    sum.add((long) 0);
                }
                sum.set(p, sum.get(p) + bits.str2long(a));
            }
        }
        userFormat res = new userFormat("|", "column|summary|average");
        long div = lst.size() - 1;
        if (div < 1) {
            div = 1;
        }
        for (int i = 0; i < sum.size(); i++) {
            long val = sum.get(i);
            res.add("col" + i + "|" + val + "|" + (val / div));
        }
        return res.formatAll(tabMod);
    }

    private List<String> doSecond(List<String> lst) {
        switch (filterF) {
            case count:
                return doCount(lst);
            case summary:
                return doSummary(lst);
            default:
                return lst;
        }
    }

    /**
     * filter the list
     *
     * @param lst list to filter
     * @return filtered list
     */
    public List<String> doFilterList(List<String> lst) {
        if (lst == null) {
            return null;
        }
        switch (filterM) {
            case include:
                List<String> res = new ArrayList<String>();
                for (int i = 0; i < lst.size(); i++) {
                    String s = lst.get(i);
                    boolean b = s.matches(filterS);
                    if (!b) {
                        continue;
                    }
                    res.add(s);
                }
                return doSecond(res);
            case exclude:
                res = new ArrayList<String>();
                for (int i = 0; i < lst.size(); i++) {
                    String s = lst.get(i);
                    boolean b = s.matches(filterS);
                    if (b) {
                        continue;
                    }
                    res.add(s);
                }
                return doSecond(res);
            case sort:
                findColumn(lst.get(0));
                if (columnB < 0) {
                    return bits.str2lst("no such column");
                }
                Collections.sort(lst, this);
                return doSecond(lst);
            case uniq:
                findColumn(lst.get(0));
                if (columnB < 0) {
                    return bits.str2lst("no such column");
                }
                res = new ArrayList<String>();
                List<String> tab = new ArrayList<String>();
                for (int i = 0; i < lst.size(); i++) {
                    String a = lst.get(i);
                    String b = a.substring(columnB, columnE);
                    if (tab.contains(b)) {
                        continue;
                    }
                    tab.add(b);
                    res.add(a);
                }
                return doSecond(res);
            case redirect:
                bits.buf2txt(true, lst, filterS);
                return lst;
            case first:
                int num = bits.str2num(filterS);
                for (int i = lst.size() - 1; i >= num; i--) {
                    lst.remove(i);
                }
                return doSecond(lst);
            case last:
                num = bits.str2num(filterS);
                for (int i = lst.size() - num; i >= 0; i--) {
                    lst.remove(i);
                }
                return doSecond(lst);
            case begin:
                num = bits.lstFnd(lst, filterS);
                if (num < 0) {
                    return new ArrayList<String>();
                }
                num--;
                for (int i = num; i >= 0; i--) {
                    lst.remove(i);
                }
                return doSecond(lst);
            case end:
                num = bits.lstFnd(lst, filterS);
                if (num < 0) {
                    return lst;
                }
                num++;
                for (int i = lst.size() - 1; i >= num; i--) {
                    lst.remove(i);
                }
                return doSecond(lst);
            case count:
                lst = doCount(lst);
                return doSecond(lst);
            case summary:
                lst = doSummary(lst);
                return doSecond(lst);
            case headers:
                lst = userFilter.getSecList(userFilter.text2section(lst), null, null);
                return doSecond(lst);
            case viewer:
                userEditor edtr = new userEditor(new userScreen(pipe), lst, "result", false);
                edtr.doView();
                return new ArrayList<String>();
            case level:
                lst = userFilter.sectionDump(userFilter.text2section(lst), userFormat.tableMode.normal);
                return doSecond(lst);
            case csv:
                lst = userFilter.sectionDump(userFilter.text2section(lst), userFormat.tableMode.csv);
                return doSecond(lst);
            case html:
                lst = userFilter.sectionDump(userFilter.text2section(lst), userFormat.tableMode.html);
                return doSecond(lst);
            case linenum:
                lst = bits.lst2lin(lst, true);
                return doSecond(lst);
            case section:
                lst = userFilter.getSection(lst, filterS);
                return doSecond(lst);
            case setdel:
                lst = userFilter.sectionDump(userFilter.text2section(lst), userFormat.tableMode.setdel);
                return doSecond(lst);
            default:
                return lst;
        }
    }

    private void doPutArr(List<String> lst, boolean need2color) {
        lst = doFilterList(lst);
        if (lst == null) {
            pipe.linePut("");
            return;
        }
        need2color &= pipe.settingsGet(pipeSetting.colors, false);
        final int height = pipe.settingsGet(pipeSetting.height, 25);
        int o = 0;
        for (int i = 0; i < lst.size(); i++) {
            if ((i == 0) && need2color) {
                userScreen.sendCol(pipe, userScreen.colBrYellow);
            }
            pipe.linePut(lst.get(i));
            if ((i == 0) && need2color) {
                userScreen.sendCol(pipe, userScreen.colWhite);
            }
            o++;
            if (o < height) {
                continue;
            }
            o = 0;
            if (height < 1) {
                continue;
            }
            switch (doMorePrompt()) {
                case 1: // normal listing
                    break;
                case 2: // next line
                    o = height - 1;
                    break;
                case 3: // end listing
                    return;
            }
        }
        pipe.linePut("");
    }

    /**
     * display one text to user
     *
     * @param lst string array to display
     */
    public void putStrArr(List<String> lst) {
        doPutArr(lst, false);
    }

    /**
     * display one text to table
     *
     * @param lst string array to display
     */
    public void putStrTab(userFormat lst) {
        if (lst == null) {
            pipe.linePut("");
            return;
        }
        doPutArr(lst.formatAll(pipe.settingsGet(pipeSetting.tabMod, userFormat.tableMode.normal)), true);
    }

    /**
     * put current line
     *
     * @param clr clear before draw
     */
    public synchronized void putCurrLine(boolean clr) {
        final String trncd = "$";
        final boolean color = pipe.settingsGet(pipeSetting.colors, false);
        final int width = pipe.settingsGet(pipeSetting.width, 80) - 1;
        clr |= rangeCheck();
        pipe.blockingPut(pipeSide.getEnding(pipeSide.modTyp.modeCR), 0, 1);
        String s = curr.substring(beg, curr.length());
        int crsr = pos - beg + prompt.length();
        if (beg > 0) {
            s = trncd + s;
            crsr += trncd.length();
        }
        int left = width - prompt.length();
        if (s.length() > left) {
            s = s.substring(0, left - trncd.length()) + trncd;
        }
        s = prompt + s;
        pipe.blockingPut(pipeSide.getEnding(pipeSide.modTyp.modeCR), 0, 1);
        if (clr) {
            pipe.strPut(bits.padEnd(s, width, " "));
        } else {
            pipe.strPut(s);
        }
        pipe.blockingPut(pipeSide.getEnding(pipeSide.modTyp.modeCR), 0, 1);
        if (color) {
            userScreen.sendCol(pipe, userScreen.colBrGreen);
        }
        pipe.strPut(s.substring(0, crsr));
        if (color) {
            userScreen.sendCol(pipe, userScreen.colWhite);
        }
    }

    private boolean rangeCheck() {
        len = curr.length();
        int old = beg;
        final int width = pipe.settingsGet(pipeSetting.width, 80) - 1;
        final int mov = width / 10;
        if (pos < 0) {
            pos = 0;
        }
        if (pos > len) {
            pos = len;
        }
        int i = pos - width + prompt.length() + mov;
        if (beg < i) {
            beg = i + mov;
        }
        i = pos - mov;
        if (beg > i) {
            beg = i - mov;
        }
        if (beg < 0) {
            beg = 0;
        }
        return (old != beg);
    }

    private String part(int beg, int end) {
        len = curr.length();
        if (beg < 0) {
            beg = 0;
        }
        if (end > len) {
            end = len;
        }
        if (beg >= end) {
            return "";
        }
        return curr.substring(beg, end);
    }

    private boolean moreChars() {
        return pipe.ready2rx() > 0;
    }

    /**
     * flush keyboard buffer
     */
    public void keyFlush() {
        for (;;) {
            byte[] buf = new byte[1];
            if (pipe.nonBlockGet(buf, 0, buf.length) != buf.length) {
                break;
            }
        }
    }

    private void beginWriting() {
        curr = "";
        pos = 0;
        beg = 0;
        histN = -1;
    }

    private void cmdLeft() {
        pos--;
    }

    private void cmdRight() {
        pos++;
    }

    private void cmdHome() {
        pos = 0;
    }

    private void cmdEnd() {
        pos = len;
    }

    private void cmdClear() {
        clip = curr;
        curr = "";
        clear = true;
    }

    private void cmdDelChr() {
        curr = part(0, pos) + part(pos + 1, len);
        clear = true;
    }

    private void cmdInsStr(String st) {
        curr = part(0, pos) + st + part(pos, len);
        pos += st.length();
        curr = part(0, 65536);
    }

    private void cmdInsChr(int ch) {
        if (ch < 32) {
            return;
        }
        if (ch > 127) {
            return;
        }
        cmdInsStr("" + ((char) ch));
    }

    private void cmdBackspace() {
        curr = part(0, pos - 1) + part(pos, len);
        clear = true;
        pos--;
    }

    private void cmdTabulator() {
        String s = help.guessLine(curr);
        if (s == null) {
            return;
        }
        curr = s;
        pos = curr.length();
        clear = true;
    }

    private void cmdErase2end() {
        clip = part(pos, len);
        curr = part(0, pos);
        clear = true;
    }

    private void cmdErase2beg() {
        clip = part(0, pos);
        curr = part(pos, len);
        pos = 0;
        clear = true;
    }

    private String cmdEnter() {
        putCurrLine(true);
        String a = curr;
        beginWriting();
        pipe.linePut("");
        clear = true;
        int i = a.indexOf(cmds.comment);
        if (i >= 0) {
            a = a.substring(0, i);
        }
        if (a.trim().length() < 1) {
            putCurrLine(true);
            return null;
        }
        if (!a.equals("" + histD[0])) {
            for (i = histD.length - 2; i >= 0; i--) {
                histD[i + 1] = histD[i];
            }
            histD[0] = a;
        }
        String b = help.repairLine(a);
        if (!help.endOfCmd(b)) {
            if (debugger.userReaderEvnt) {
                logger.debug("got " + prompt + b);
            }
            if (pipe.settingsGet(pipeSetting.logging, false)) {
                logger.info("command " + prompt + b + " from " + pipe.settingsGet(pipeSetting.origin, "?"));
            }
            return b;
        }
        putStrArr(help.getHelp(a, true));
        putCurrLine(true);
        return null;
    }

    private void cmdRefreshLine() {
        pipe.linePut("");
        putCurrLine(true);
    }

    private void cmdHistNext() {
        if (histN < 0) {
            curr = "";
            histN = 0;
        } else {
            curr = histD[histN];
        }
        histN--;
        pos = curr.length();
        clear = true;
    }

    private void cmdHistPrev() {
        histN++;
        if (histN >= histD.length) {
            histN = histD.length - 1;
        }
        curr = histD[histN];
        if (curr.length() < 1) {
            histN--;
        }
        pos = curr.length();
        clear = true;
    }

    private void cmdSwapLetters() {
        curr = part(0, pos - 1) + part(pos, pos + 1) + part(pos - 1, pos)
                + part(pos + 1, len);
        pos++;
    }

    private void cmdSpecChr() {
        int i = userScreen.getKey(pipe);
        if ((i & 0x8000) != 0) { // special
            return;
        }
        if ((i & 0x200) != 0) { // ctrl
            i &= 0x1f;
        }
        cmdInsChr(i & 0xff);
    }

    private void cmdEraseBack() {
        int i = cmds.wordBound(curr, pos - 1, -1);
        if (i != 0) {
            i++;
        }
        clip = part(i, pos);
        curr = part(0, i) + part(pos, len);
        pos = i;
        clear = true;
    }

    private void cmdEraseFwrd() {
        int i = cmds.wordBound(curr, pos, +1);
        clip = part(pos, i);
        curr = part(0, pos) + part(i, len);
        clear = true;
    }

    private void cmdShowHelp() {
        putCurrLine(true);
        pipe.linePut("?");
        List<String> l = help.getHelp(curr, false);
        Collections.sort(l);
        putStrArr(l);
        putCurrLine(true);
    }

    private void cmdBackward() {
        pos = cmds.wordBound(curr, pos - 1, -1);
        if (pos != 0) {
            pos++;
        }
    }

    private void cmdForward() {
        pos = cmds.wordBound(curr, pos, +1);
    }

    private void cmdCapitalize() {
        int i = cmds.wordBound(curr, pos, +1);
        String s = part(pos, i);
        if (s.length() < 1) {
            return;
        }
        s = s.substring(0, 1).toUpperCase()
                + s.substring(1, s.length()).toLowerCase();
        curr = part(0, pos) + s + part(i, len);
        pos = i;
    }

    private void cmdLowercase() {
        int i = cmds.wordBound(curr, pos, +1);
        curr = part(0, pos) + part(pos, i).toLowerCase() + part(i, len);
        pos = i;
    }

    private void cmdUppercase() {
        int i = cmds.wordBound(curr, pos, +1);
        curr = part(0, pos) + part(pos, i).toUpperCase() + part(i, len);
        pos = i;
    }

    /**
     * read up one line
     *
     * @param exit exit command to return
     * @return string readed, null if error happened
     */
    public String readLine(String exit) {
        final int deactivate = pipe.settingsGet(pipeSetting.deactive, 65536);
        final boolean spacetab = pipe.settingsGet(pipeSetting.spacTab, false);
        setFilter(null);
        if (debugger.userReaderEvnt) {
            logger.debug("reading");
        }
        beginWriting();
        putCurrLine(true);
        for (;;) {
            clear = false;
            String oldS = "" + curr;
            int oldP = pos;
            for (;;) {
                len = curr.length();
                int ch = userScreen.getKey(pipe);
                if (ch == deactivate) {
                    return null;
                }
                switch (ch) {
                    case -1:
                        if (debugger.userReaderEvnt) {
                            logger.debug("closed");
                        }
                        return null;
                    case 0x8003: // backspace
                        cmdBackspace();
                        break;
                    case 0x8002: // tabulator
                        cmdTabulator();
                        break;
                    case 0x8004: // enter
                        String s = cmdEnter();
                        if (s == null) {
                            break;
                        }
                        return s;
                    case 0x800e: // left
                        cmdLeft();
                        break;
                    case 0x800f: // right
                        cmdRight();
                        break;
                    case 0x800c: // up
                        cmdHistPrev();
                        break;
                    case 0x800d: // down
                        cmdHistNext();
                        break;
                    case 0x8008: // home                
                        cmdHome();
                        break;
                    case 0x8009: // end
                        cmdEnd();
                        break;
                    case 0x8007: // delete
                        cmdDelChr();
                        break;
                    case 0x261: // ctrl + a
                        cmdHome();
                        break;
                    case 0x262: // ctrl + b
                        cmdLeft();
                        break;
                    case 0x263: // ctrl + c
                        cmdClear();
                        break;
                    case 0x264: // ctrl + d
                        cmdDelChr();
                        break;
                    case 0x265: // ctrl + e
                        cmdEnd();
                        break;
                    case 0x266: // ctrl + f
                        cmdRight();
                        break;
                    case 0x26b: // ctrl + k
                        cmdErase2end();
                        break;
                    case 0x26c: // ctrl + l
                        cmdRefreshLine();
                        break;
                    case 0x26e: // ctrl + n
                        cmdHistNext();
                        break;
                    case 0x270: // ctrl + p
                        cmdHistPrev();
                        break;
                    case 0x271: // ctrl + q
                        cmdSpecChr();
                        break;
                    case 0x272: // ctrl + r
                        cmdRefreshLine();
                        break;
                    case 0x274: // ctrl + t
                        cmdSwapLetters();
                        break;
                    case 0x275: // ctrl + u
                        cmdErase2beg();
                        break;
                    case 0x276: // ctrl + v
                        cmdSpecChr();
                        break;
                    case 0x277: // ctrl + w
                        cmdEraseBack();
                        break;
                    case 0x278: // ctrl + x
                        cmdErase2beg();
                        break;
                    case 0x279: // ctrl + y
                        cmdInsStr(clip);
                        break;
                    case 0x27a: // ctrl + z
                        cmdRefreshLine();
                        cmdClear();
                        if (exit == null) {
                            break;
                        }
                        return exit;
                    case 0x462: // alt + b
                        cmdBackward();
                        break;
                    case 0x463: // alt + c
                        cmdCapitalize();
                        break;
                    case 0x464: // alt + d
                        cmdEraseFwrd();
                        break;
                    case 0x466: // alt + f
                        cmdForward();
                        break;
                    case 0x46c: // alt + l
                        cmdLowercase();
                        break;
                    case 0x471: // alt + q
                        cmdSpecChr();
                        break;
                    case 0x475: // alt + u
                        cmdUppercase();
                        break;
                    case 32: // space
                        if (spacetab && (pos >= len)) {
                            cmdTabulator();
                        } else {
                            cmdInsChr(ch);
                        }
                        break;
                    case 63: // ? question mark
                        cmdShowHelp();
                        break;
                    default:
                        if ((ch & 0xff) != ch) {
                            break;
                        }
                        cmdInsChr(ch);
                        break;
                }
                if (!moreChars()) {
                    break;
                }
                rangeCheck();
            }
            if (clear) {
                putCurrLine(true);
                continue;
            }
            if ((!oldS.equals(curr)) || (pos != oldP)) {
                putCurrLine(clear);
            }
        }
    }

    /**
     * convert user filter to regexp
     *
     * @param flt user filter
     * @return regular expression
     */
    public static String filter2reg(String flt) {
        flt = ".*" + flt + ".*";
        flt = flt.replaceAll(" ", ".*");
        flt = flt.replaceAll("\\|", ".*|.*");
        return flt;
    }

    /**
     * set filter string
     *
     * @param cmd filter string, null if nothing
     * @return updated command
     */
    public cmds setFilter(cmds cmd) {
        if (debugger.userReaderEvnt) {
            logger.debug("set filter");
        }
        filterS = "";
        filterM = mode.raw;
        filterF = mode.raw;
        if (cmd == null) {
            return null;
        }
        String a = cmd.getRemaining();
        int i = a.indexOf("|");
        if (i < 0) {
            return cmd;
        }
        cmd = new cmds("exec", a.substring(0, i - 1).trim());
        a = a.substring(i + 1, a.length()).trim();
        i = a.indexOf(" ");
        if (i < 0) {
            if (a.equals("headers")) {
                filterM = mode.headers;
                return cmd;
            }
            if (a.equals("count")) {
                filterM = mode.count;
                return cmd;
            }
            if (a.equals("summary")) {
                filterM = mode.summary;
                return cmd;
            }
            if (a.equals("viewer")) {
                filterM = mode.viewer;
                return cmd;
            }
            if (a.equals("csv")) {
                filterM = mode.csv;
                return cmd;
            }
            if (a.equals("html")) {
                filterM = mode.html;
                return cmd;
            }
            if (a.equals("setdel")) {
                filterM = mode.setdel;
                return cmd;
            }
            if (a.equals("level")) {
                filterM = mode.level;
                return cmd;
            }
            if (a.equals("linenumbers")) {
                filterM = mode.linenum;
                return cmd;
            }
            return cmd;
        }
        filterS = a.substring(i, a.length()).trim();
        a = a.substring(0, i).trim();
        i = filterS.lastIndexOf(" | ");
        if (i > 0) {
            String s = filterS.substring(i + 3, filterS.length()).trim();
            filterS = filterS.substring(0, i);
            if (s.equals("count")) {
                filterF = mode.count;
            }
            if (s.equals("summary")) {
                filterF = mode.summary;
            }
        }
        if (a.equals("include")) {
            filterS = filter2reg(filterS);
            filterM = mode.include;
            return cmd;
        }
        if (a.equals("exclude")) {
            filterS = filter2reg(filterS);
            filterM = mode.exclude;
            return cmd;
        }
        if (a.equals("begin")) {
            filterS = filter2reg(filterS);
            filterM = mode.begin;
            return cmd;
        }
        if (a.equals("end")) {
            filterS = filter2reg(filterS);
            filterM = mode.end;
            return cmd;
        }
        if (a.equals("redirect")) {
            filterM = mode.redirect;
            return cmd;
        }
        if (a.equals("sort")) {
            filterM = mode.sort;
            return cmd;
        }
        if (a.equals("uniq")) {
            filterM = mode.uniq;
            return cmd;
        }
        if (a.equals("section")) {
            filterS = filter2reg(filterS);
            filterM = mode.section;
            return cmd;
        }
        if (a.equals("first")) {
            filterM = mode.first;
            return cmd;
        }
        if (a.equals("last")) {
            filterM = mode.last;
            return cmd;
        }
        if (a.equals("reginc")) {
            filterM = mode.include;
            return cmd;
        }
        if (a.equals("regexc")) {
            filterM = mode.exclude;
            return cmd;
        }
        if (a.equals("regbeg")) {
            filterM = mode.begin;
            return cmd;
        }
        if (a.equals("regend")) {
            filterM = mode.end;
            return cmd;
        }
        if (a.equals("regsec")) {
            filterM = mode.section;
            return cmd;
        }
        return cmd;
    }

}
