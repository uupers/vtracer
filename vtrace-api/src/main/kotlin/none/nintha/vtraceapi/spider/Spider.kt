package none.nintha.vtraceapi.spider

import none.nintha.vtraceapi.service.BiliService
import none.nintha.vtraceapi.util.HttpSender
import okhttp3.FormBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

data class Spider(val proxyIp: String, val proxyPort: Int) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
        const val SLEEP_TIME_MILLIS: Long = 220
        const val STATUS_FREE = 0
        const val STATUS_RUNNING = 1
        const val MAX_FAIL_TIMES = 2
    }


    var status: Int = STATUS_FREE
    var nextRuntime: LocalDateTime = LocalDateTime.now()
    var failTimes: Int = 0

    fun getProxyAddress() = "$proxyIp:$proxyPort"


    fun fetchMember(mid: Long): String {
        if (status != STATUS_FREE) {
            logger.warn("[Spider Fetch] Oops, none.nintha.vtraceapi.spider(${getProxyAddress()}) is busy.")
            return ""
        }
        status = STATUS_RUNNING
        val sleepTime = Duration.between(LocalDateTime.now(), nextRuntime).toNanos() / (1000 * 1000)
        if (sleepTime > 0) Thread.sleep(sleepTime)
        nextRuntime = LocalDateTime.now().plusNanos(1000 * 1000 * SLEEP_TIME_MILLIS)

        val formBody = FormBody.Builder().add("mid", mid.toString()).build()
        var html = HttpSender.post(BiliService.POST_MEMBER_API, formBody, proxyIp, proxyPort)
        if (html == "" || html.contains("<!DOCTYPE HTML")) {
            failTimes++
            html = ""
        } else failTimes = 0


        status = STATUS_FREE
        return html
    }

    fun fetchCard(mid: Long): String {
        if (status != STATUS_FREE) {
            logger.warn("[Spider Fetch] Oops, none.nintha.vtraceapi.spider(${getProxyAddress()}) is busy.")
            return ""
        }
        status = STATUS_RUNNING
        val sleepTime = Duration.between(LocalDateTime.now(), nextRuntime).toNanos() / (1000 * 1000)
        if (sleepTime > 0) Thread.sleep(sleepTime)
        nextRuntime = LocalDateTime.now().plusNanos(1000 * 1000 * SLEEP_TIME_MILLIS)

        val url = "${BiliService.GET_CARD_API}?mid=$mid"
        var html = HttpSender.get(url, proxyIp, proxyPort)
        if (html == "" || html.startsWith("<")) {
            failTimes++
            html = ""
        } else failTimes = 0


        status = STATUS_FREE
        return html
    }
}

//fun main(args: Array<String>) {
//    val now = LocalDateTime.now()
//    val after = now.plusNanos(1000 * 1000 * Spider.SLEEP_TIME_MILLIS)
//    val millis = Duration.between(LocalDateTime.now(), after).toNanos() / (1000 * 1000)
//    println(millis)
//}