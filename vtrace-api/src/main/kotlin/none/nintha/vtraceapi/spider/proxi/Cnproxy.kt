package none.nintha.bilifetcher.proxi

import none.nintha.vtraceapi.util.HttpSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Cnproxy :Proci{
    override val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val urls = listOf("http://cn-proxy.com/", "http://cn-proxy.com/archives/218")
    private val regex = Regex("""<tr>\n<td>([.0-9]+)<\/td>\n<td>([0-9]+)<\/td>""")
    override fun fetch(): Set<String> {
        val list = urls.flatMap {
            val html = HttpSender.get(it)
            regex.findAll(html).map { "${it.groups[1]!!.value}:${it.groups[2]!!.value}" }.toList()
        }
        return list.toSet()
    }
}

//fun main(args: Array<String>) {
//    val proxi = Cnproxy()
//    val proxys = proxi.fetchAndCheck()
//    println("proxies size=${proxys.size}")
//    LoggerFactory.getLogger("Cnproxy").info(proxys.toString())
//}