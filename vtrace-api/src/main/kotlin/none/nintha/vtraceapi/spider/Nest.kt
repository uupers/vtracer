package none.nintha.vtraceapi.spider

import none.nintha.vtraceapi.util.HttpSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ThreadLocalRandom

class Nest {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
        val PROXY_MAP: ConcurrentHashMap<String, Spider> = ConcurrentHashMap()
        val MAIN_QUEUE: Queue<Spider> = ConcurrentLinkedQueue()

        fun addProxys(proxys: Collection<String>) {
            val num = proxys.toSet().asSequence().filter { !PROXY_MAP.containsKey(it) }.map {
                val parts = it.split(":")
                Spider(parts[0], parts[1].toInt())
            }.map { PROXY_MAP[it.getProxyAddress()] = it;MAIN_QUEUE.offer(it) }.count()
            logger.info("[Nest add proxys] num=$num, total=${PROXY_MAP.size}")
        }

        fun getSpider(): Spider {
            while (true) {
                val spider: Spider? = MAIN_QUEUE.poll()
                if (spider == null) {
                    Thread.sleep(ThreadLocalRandom.current().nextLong(200) + 50)
                    continue
                }
                if (spider.failTimes >= Spider.MAX_FAIL_TIMES) {
                    PROXY_MAP.remove(spider.getProxyAddress())
                    continue
                }
                return spider
            }
        }

        fun returnSpider(spider: Spider) {
            if(spider.proxyIp == HttpSender.LOCALHOST) return

            logger.debug("[Nest] return none.nintha.vtraceapi.spider=${spider.getProxyAddress()}")
            MAIN_QUEUE.offer(spider)
        }

        fun isEmpty(): Boolean = MAIN_QUEUE.isEmpty()

        fun isNonInNest(proxyAddress: String): Boolean {
            return !PROXY_MAP.containsKey(proxyAddress)
        }
    }
}