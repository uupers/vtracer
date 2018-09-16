package none.nintha.vtraceapi.entity.consts

class ApiGen {
    companion object {
        fun memberSubmit(mid: Long): String = "http://space.bilibili.com/ajax/member/getSubmitVideos?mid=${mid}&pagesize=1&page=1&order=pubdate"
        fun videoStatBatch(aids: List<Long>):String = "http://api.bilibili.com/x/article/archives?ids=${aids.joinToString(",")}"
        fun videoStat(aid:Long):String = "http://api.bilibili.com/x/web-interface/archive/stat?aid=${aid}"
        fun videoHtml(aid:Long):String = "http://www.bilibili.com/video/av${aid}"
        fun memberTotalView(mid:Long):String = "http://api.bilibili.com/x/space/upstat?mid=${mid}"
        fun memberInfo(mid:Long):String = "http://api.bilibili.com/x/web-interface/card?mid=${mid}"
    }
}