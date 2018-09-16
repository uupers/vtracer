package none.nintha.vtraceapi.entity

data class PartCard(var mid: Long) {
    var fans: Long = 0
    var name: String = ""
    var face: String = ""
    var attention: Long = 0
    var sex: Int = 2
    var sign: String = ""
    var archive: Long = 0
    var rtime: Long = 0 // 爬取的时间，时间戳格式
    var taskId: Long = 0 // 爬取任务的ID

    fun getFullFace():String = if(face.startsWith("http")) face else "http://i0.hdslb.com/bfs/face/$face"
}