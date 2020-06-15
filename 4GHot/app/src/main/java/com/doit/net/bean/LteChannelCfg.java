package com.doit.net.bean;

/**
 * Created by Zxc on 2018/10/19.
 */

public class LteChannelCfg {
    //这里是协议里直接有的
    private String idx;
    private String band;
    private String fcn;
    private String plmn;
    private String pa;
    private String ga;
    private String pw;
    private String rlm;
    private String change = "";
    private String autoopen;
    private String alt_fcn;
    private String pmax;

    //这条协议之外的值
   private boolean RFState;


    //这里暂时还不知道什么意思
    private Integer state;
    public Boolean hold;
    public String info;
    private int[] fcns;
    private int[] pas;
    private float[] pwrs;
    private boolean result;


    private String mac;
    private String fw;
    private String gps;
    private String cnm;
    private String sync;
    private String cell;
    private String rip;
    private String tmp;
    private String ulfcn;
    private String ip;
    private String gain;
    private String rxGain;
    private String pci;
    private String period;
    private String gpsOffset;
    private String pollTmr;
    private String tac;

    public String getTac() {
        return tac;
    }

    public void setTac(String tac) {
        this.tac = tac;
    }

    public String getPollTmr() {
        return pollTmr;
    }

    public void setPollTmr(String pollTmr) {
        this.pollTmr = pollTmr;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getGpsOffset() {
        return gpsOffset;
    }

    public void setGpsOffset(String gpsOffset) {
        this.gpsOffset = gpsOffset;
    }

    public String getPci() {
        return pci;
    }

    public void setPci(String pci) {
        this.pci = pci;
    }

    public String getGain() {
        return gain;
    }

    public void setGain(String gain) {
        this.gain = gain;
    }

    public String getRxGain() {
        return rxGain;
    }

    public void setRxGain(String rxGain) {
        this.rxGain = rxGain;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public String getAutoopen() {
        return autoopen;
    }

    public void setAutoopen(String autoopen) {
        this.autoopen = autoopen;
    }

    public String getAlt_fcn() {
        return alt_fcn;
    }

    public void setAlt_fcn(String alt_fcn) {
        this.alt_fcn = alt_fcn;
    }

    public String getPmax() {
        return pmax;
    }

    public void setPmax(String pmax) {
        this.pmax = pmax;
    }

    public boolean isRFState() {
        return RFState;
    }

    public void setRFState(boolean RFState) {
        this.RFState = RFState;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getFw() {
        return fw;
    }

    public void setFw(String fw) {
        this.fw = fw;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        this.gps = gps;
    }

    public String getCnm() {
        return cnm;
    }

    public void setCnm(String cnm) {
        this.cnm = cnm;
    }

    public String getSync() {
        return sync;
    }

    public void setSync(String sync) {
        this.sync = sync;
    }

    public String getCell() {
        return cell;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }

    public String getRip() {
        return rip;
    }

    public void setRip(String rip) {
        this.rip = rip;
    }

    public String getTmp() {
        return tmp;
    }

    public void setTmp(String tmp) {
        this.tmp = tmp;
    }

    public String getUlfcn() {
        return ulfcn;
    }

    public void setUlfcn(String ulfcn) {
        this.ulfcn = ulfcn;
    }

    public LteChannelCfg() {
    }

    public String getIdx() {
        return this.idx;
    }
    public void setIdx(String idx) {
        this.idx = idx;
    }

    public String getBand() {
        return this.band;
    }
    public void setBand(String band) {
        this.band = band;
    }

    public String getFcn() {
        return this.fcn;
    }
    public void setFcn(String fcn) {
        this.fcn = fcn;
    }

    public String getPlmn() {
        return this.plmn;
    }
    public void setPlmn(String plmn) {
        this.plmn = plmn;
    }

    public String getPa() {
        return this.pa;
    }
    public void setPa(String pa) {
        this.pa = pa;
    }

    public boolean get() {
        return this.RFState;
    }
    public void setState(boolean state) {
        this.RFState = state;
    }

    public String getGa() {
        return this.ga;
    }
    public void setGa(String ga) {
        this.ga = ga;
    }

    public String getPw() {
        return this.pw;
    }
    public void setPw(String pw) {
        this.pw = pw;
    }

    public boolean isRfOpen() {
        return this.state != null && this.state.intValue() == 1;
    }

    public String getPMax() {
        return pmax;
    }

    public void setPMax(String max) {
        this.pmax = max;
    }

    public Boolean getHold() {
        return this.hold;
    }
    public void setHold(Boolean hold) {
        this.hold = hold;
    }

    public boolean isResult() {
        return this.result;
    }
    public void setResult(boolean result) {
        this.result = result;
    }

    public String getRlm() {
        return this.rlm;
    }
    public void setRlm(String rxlevmin) {
        this.rlm = rxlevmin;
    }

    public String getInfo() {
        return this.info;
    }
    public void setInfo(String info) {
        this.info = info;
    }

    public int[] getFcns() {
        return this.fcns;
    }
    public void setFcns(int[] fcns) {
        this.fcns = fcns;
    }

    public int[] getPas() {
        return this.pas;
    }
    public void setPas(int[] pas) {
        this.pas = pas;
    }

    public String getFcnsString() {
        if(this.fcns == null) {
            return null;
        } else {
            StringBuilder s = new StringBuilder();
            int[] var2 = this.fcns;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                int fcn = var2[var4];
                s.append(fcn + ",");
            }

            if(s.length() > 0) {
                s.delete(s.length() - 1, s.length());
            }

            return s.toString();
        }
    }

    public float[] getPwrs() {
        return this.pwrs;
    }
    public void setPwrs(float[] pwrs) {
        this.pwrs = pwrs;
    }


    public String getChangeBand() {
        return change;
    }

    public void setChangeBand(String change) {
        this.change = change;
    }

    public String getAutoOpen() {
        return autoopen;
    }

    public void setAutoOpen(String autoopen) {
        this.autoopen = autoopen;
    }

    public String getAltFcn() {
        return alt_fcn;
    }

    public void setAltFcn(String alt_fcn) {
        this.alt_fcn = alt_fcn;
    }
}
