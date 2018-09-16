package none.nintha.vtraceapi.spider

data class FetchTask(var mid: Long = 0, var status: Int = STATUS_WAITING) {
    companion object {
        val STATUS_RUNNING = -1
        val STATUS_WAITING = 0
        val STATUS_FINISHED = 1
    }

    override fun toString(): String {
        return "FetchTask(mid=$mid, status=$status)"
    }
}