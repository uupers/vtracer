package none.nintha.vtraceapi.entity

data class TraceTaskConfig(var id: Long = 0) {
    var totalRound: Int = 0 //总轮次
    var currentRound: Int = 0
    var roundStatus: Int = 0 // 0-stopped, 1-running, 2-exporting, 3-stopping

}