package none.nintha.vtraceapi.entity

class Results<T>(var code: Int = 0, var message: String = "success",var data: T? = null) {
    companion object {
        @JvmStatic
        fun <T> success(data : T? = null): Results<T>{
            return Results(data = data)
        }
        @JvmStatic
        fun <T> failed(errorMsg: String): Results<T> {
            return Results(400, errorMsg)
        }
        @JvmStatic
        fun <T> unlogin(): Results<T>{
            return Results(401, "未登录，请登录后再试")
        }
    }


}