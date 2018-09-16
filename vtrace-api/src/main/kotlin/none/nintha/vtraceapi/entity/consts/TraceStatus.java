package none.nintha.vtraceapi.entity.consts;

import java.util.Arrays;
import java.util.Optional;

public enum TraceStatus {
    STOPPED(0, "已停止"), RUNNING(1, "运行中");
    public int code;
    public String desc;

    TraceStatus(int c, String d) {
        code = c;
        desc = d;
    }

    public static Optional<TraceStatus> ofCode(int code){
         return Arrays.stream(TraceStatus.values()).filter(e -> e.code == code).findAny();
    }
}
