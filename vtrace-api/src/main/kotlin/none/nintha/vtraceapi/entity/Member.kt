package none.nintha.vtraceapi.entity

import com.fasterxml.jackson.annotation.JsonIgnore

data class Member(var mid: Long = 0){
    var name: String = ""
    @JsonIgnore
    var sex: Int = 2
    var face: String = ""
    var regtime: Long = 0
    var level: Int = 0
    var sign: String = ""
    var birthday: String = ""

    override fun toString(): String {
        return "Member(mid=$mid, name='$name', sex=$sex, face='$face', regtime=$regtime, level=$level, sign='$sign', birthday='$birthday')"
    }
}