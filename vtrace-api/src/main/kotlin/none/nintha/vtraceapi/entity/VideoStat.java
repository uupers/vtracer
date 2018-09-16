package none.nintha.vtraceapi.entity;

import com.google.common.base.Objects;

import java.util.Date;

public class VideoStat {
    private long aid;
    private long view;
    private long danmaku;
    private long favorite;
    private long reply;
    private long coin;
    private long share;
    private long like;
    private long dislike;
    // 写入mongo的时间
    private Date ctime;

    public long getFavorite() {
        return favorite;
    }

    public void setFavorite(long favorite) {
        this.favorite = favorite;
    }

    public long getAid() {
        return aid;
    }

    public void setAid(long aid) {
        this.aid = aid;
    }

    public long getView() {
        return view;
    }

    public void setView(long view) {
        this.view = view;
    }

    public long getDanmaku() {
        return danmaku;
    }

    public void setDanmaku(long danmaku) {
        this.danmaku = danmaku;
    }

    public long getReply() {
        return reply;
    }

    public void setReply(long reply) {
        this.reply = reply;
    }

    public long getCoin() {
        return coin;
    }

    public void setCoin(long coin) {
        this.coin = coin;
    }

    public long getShare() {
        return share;
    }

    public void setShare(long share) {
        this.share = share;
    }

    public Date getCtime() {
        return ctime;
    }

    public void setCtime(Date ctime) {
        this.ctime = ctime;
    }

    public long getLike() {
        return like;
    }

    public void setLike(long like) {
        this.like = like;
    }

    public long getDislike() {
        return dislike;
    }

    public void setDislike(long dislike) {
        this.dislike = dislike;
    }

    /**
     * check fields exclude ctime
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoStat videoStat = (VideoStat) o;
        return aid == videoStat.aid &&
                view == videoStat.view &&
                danmaku == videoStat.danmaku &&
                favorite == videoStat.favorite &&
                reply == videoStat.reply &&
                coin == videoStat.coin &&
                like == videoStat.like &&
                share == videoStat.share &&
                dislike == videoStat.dislike;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(aid, view, danmaku, favorite, reply, coin, share, like, dislike);
    }

    @Override
    public String toString() {
        return "VideoStat{" +
                "aid=" + aid +
                ", view=" + view +
                ", danmaku=" + danmaku +
                ", favorite=" + favorite +
                ", reply=" + reply +
                ", coin=" + coin +
                ", share=" + share +
                ", like=" + like +
                ", dislike=" + dislike +
                ", ctime=" + ctime +
                '}';
    }
}
