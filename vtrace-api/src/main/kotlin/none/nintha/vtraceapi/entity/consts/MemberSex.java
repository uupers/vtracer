package none.nintha.vtraceapi.entity.consts;

import java.util.Arrays;
import java.util.Optional;

public enum MemberSex {
    MALE(0, "男"), FEMALE(1, "女"), UNKNOWN(2, "保密");
    public int code;
    public String desc;

    MemberSex(int c, String d) {
        code = c;
        desc = d;
    }

    public static Optional<MemberSex> ofCode(int code) {
        return Arrays.stream(MemberSex.values()).filter(e -> e.code == code).findAny();
    }

    public static Optional<MemberSex> ofDesc(String desc) {
        return Arrays.stream(MemberSex.values()).filter(e -> e.desc == desc).findAny();
    }
}