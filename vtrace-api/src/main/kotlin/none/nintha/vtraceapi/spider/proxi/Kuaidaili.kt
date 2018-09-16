package none.nintha.bilifetcher.proxi

import none.nintha.vtraceapi.util.HttpSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Kuaidaili : Proci {
    override val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val inha = (1..10).map { "https://www.kuaidaili.com/free/inha/$it/" }
    private val intr = (1..10).map { "https://www.kuaidaili.com/free/intr/$it/" }
    private val urls = inha + intr
    private val regex = Regex("""<td data-title="IP">([0-9\.]+?)</td>[\s\n\r]+?<td data-title="PORT">([0-9]*?)</td>""")
    override fun fetch(): Set<String> {
        val list = urls.flatMap {
            val html = HttpSender.get(it)
            Thread.sleep(1000)
            regex.findAll(html).map { "${it.groups[1]!!.value}:${it.groups[2]!!.value}" }.toList()
        }
        return list.toSet()
    }

}

//fun main(args: Array<String>) =runBlocking<Unit>{
//    val proxi = Kuaidaili()
//    val proxys = proxi.fetchAndCheck()
//    println("proxies size=${proxys.size}")
//    LoggerFactory.getLogger("Kuaidaili").info(proxys.toString())
//
//}
