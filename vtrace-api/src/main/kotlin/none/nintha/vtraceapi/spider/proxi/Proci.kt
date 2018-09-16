package none.nintha.bilifetcher.proxi

import none.nintha.vtraceapi.spider.Nest
import none.nintha.vtraceapi.util.HttpSender
import org.apache.logging.log4j.util.Strings
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

interface Proci {
    val logger: Logger
    fun fetch(): Set<String>

    /**
     * 校验代理地址是否可用
     */
    fun check(proxyAddress: String): Boolean {
        try {
            val strs = proxyAddress.split(":")
            val html = HttpSender.get(HttpSender.TEST_URL, strs[0], strs[1].toInt())
            return Strings.isNotBlank(html)
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 获取并校验
     */
    fun fetchAndCheck(): Set<String> {
        val proxys = this.fetch().filter(Nest.Companion::isNonInNest).map {
            // 并行检查，提高效率
            CompletableFuture.supplyAsync(Supplier { Pair(it, this.check(it)) }, HttpSender.threadPool)
        }.asSequence().map { it.get() }.filter { it.second }.map { it.first }.toSet()
        logger.info("[Fetch Check] ${this::class.java.simpleName} => ${proxys.size}")
        return proxys
    }
}