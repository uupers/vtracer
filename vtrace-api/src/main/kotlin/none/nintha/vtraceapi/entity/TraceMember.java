package none.nintha.vtraceapi.entity;

import java.util.Date;

public class TraceMember {
    private long mid;
    private String name;
    private String face;
    private Integer keep = 0;
    private Date ctime;

    public long getMid() {
        return mid;
    }

    public void setMid(long mid) {
        this.mid = mid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFace() {
        return face;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public Date getCtime() {
        return ctime;
    }

    public void setCtime(Date ctime) {
        this.ctime = ctime;
    }

    public Integer getKeep() {
        return keep == null ? 0 : keep;
    }

    public void setKeep(Integer keep) {
        this.keep = keep;
    }
}
