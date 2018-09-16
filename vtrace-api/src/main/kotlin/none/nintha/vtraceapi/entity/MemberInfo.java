package none.nintha.vtraceapi.entity;

import com.google.common.base.Objects;
import org.springframework.beans.BeanUtils;

import java.util.Date;

public class MemberInfo {
    private long mid;
    private long fans;
    private long archiveView;
    private Date ctime;

    public static MemberInfo ofPartCard(PartCard card){
        MemberInfo info = new MemberInfo();
        BeanUtils.copyProperties(card, info);
        info.setCtime(new Date());
        return info;
    }

    public long getMid() {
        return mid;
    }

    public void setMid(long mid) {
        this.mid = mid;
    }

    public long getFans() {
        return fans;
    }

    public void setFans(long fans) {
        this.fans = fans;
    }

    public Date getCtime() {
        return ctime;
    }

    public void setCtime(Date ctime) {
        this.ctime = ctime;
    }

    public long getArchiveView() {
        return archiveView;
    }

    public void setArchiveView(long archiveView) {
        this.archiveView = archiveView;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberInfo that = (MemberInfo) o;
        return mid == that.mid &&
                fans == that.fans &&
                archiveView == that.archiveView;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mid, fans, archiveView);
    }
}
