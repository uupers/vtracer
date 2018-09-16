package none.nintha.vtraceapi.util;

public class CommonUtil {
    public static long parseLong(String s, long defaultValue) {
        return s.chars().allMatch(Character::isDigit) ? Long.parseLong(s) : defaultValue;
    }

}
