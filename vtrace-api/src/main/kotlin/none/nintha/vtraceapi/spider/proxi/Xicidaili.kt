package none.nintha.bilifetcher.proxi

import none.nintha.vtraceapi.util.HttpSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Xicidaili : Proci {
    override val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val urls = listOf(
            "http://www.xicidaili.com/",
            "http://www.xicidaili.com/nn/",
            "http://www.xicidaili.com/nt/",
            "http://www.xicidaili.com/wn/",
            "http://www.xicidaili.com/wt"
    )
    val regex = Regex("""<td>([0-9\.]+?)</td>[\s\n\r]+?<td>([0-9]*?)</td>""")
    override fun fetch(): Set<String> {
        return urls.flatMap {
            val html = HttpSender.get(it)
            regex.findAll(html).map { "${it.groups[1]!!.value}:${it.groups[2]!!.value}" }.toList()
        }.toSet()
    }
}

//fun main(args: Array<String>) {
//    val proxi = Xicidaili()
//    val proxys = proxi.fetchAndCheck()
//    println("proxies size=${proxys.size}")
//    LoggerFactory.getLogger("Xicidaili").info(proxys.toString())
//}
