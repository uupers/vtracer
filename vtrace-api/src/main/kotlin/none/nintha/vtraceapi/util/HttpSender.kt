package none.nintha.vtraceapi.util

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit


class HttpSender {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(HttpSender::class.java)
        const val THREAD_POOL_SIZE = 200
        val threadPool: ExecutorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE)
        const val LOCALHOST = "127.0.0.1"
        const val TEST_URL = "http://api.bilibili.com/x/web-interface/card?mid=128"
        //        const val TEST_URL = "http://api.bilibili.com/x/web-interface/archive/stat?aid=1"//"http://ip.taobao.com/service/getIpInfo.php?ip=127.0.0.1"//"http://ip-api.com/json"
        private val USER_AGENTS = listOf(
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8.0.12) Gecko/20070731 Ubuntu/dapper-security Firefox/1.5.0.12",
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Acoo Browser; SLCC1; .NET CLR 2.0.50727; Media Center PC 5.0; .NET CLR 3.0.04506)",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.20 (KHTML, like Gecko) Chrome/19.0.1036.7 Safari/535.20",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.8) Gecko Fedora/1.9.0.8-1.fc10 Kazehakase/0.5.6",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.71 Safari/537.1 LBBROWSER",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET CLR 2.0.50727; Media Center PC 6.0) ,Lynx/2.8.5rel.1 libwww-FM/2.14 SSL-MM/1.4.1 GNUTLS/1.2.9",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; QQBrowser/7.0.3698.400)",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 732; .NET4.0C; .NET4.0E)",
                "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:2.0b13pre) Gecko/20110307 Firefox/4.0b13pre",
                "Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8; U; fr) Presto/2.9.168 Version/11.52",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8.0.12) Gecko/20070731 Ubuntu/dapper-security Firefox/1.5.0.12",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; LBBROWSER)",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.8) Gecko Fedora/1.9.0.8-1.fc10 Kazehakase/0.5.6",
                "Mozilla/5.0 (X11; U; Linux; en-US) AppleWebKit/527+ (KHTML, like Gecko, Safari/419.3) Arora/0.6",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; QQBrowser/7.0.3698.400)",
                "Opera/9.25 (Windows NT 5.1; U; en), Lynx/2.8.5rel.1 libwww-FM/2.14 SSL-MM/1.4.1 GNUTLS/1.2.9",
                "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36"
        )

        private val commonClient: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(2000, TimeUnit.MILLISECONDS).build()
        private val proxyClient: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(2000, TimeUnit.MILLISECONDS).build()

        /**
         * 向指定URL发送GET方法的请求
         *
         * @param url 发送请求的URL
         * @param proxyIp 代理IP
         * @param proxyPort 代理端口
         * @param retry 重试次数
         * @return  所代表远程资源的响应结果
         */
        fun get(url: String, proxyIp: String = LOCALHOST, proxyPort: Int = 0, retry: Int = 0): String {
            val client: OkHttpClient = if (proxyIp == LOCALHOST || proxyPort == 0) {
                commonClient
            } else {
                val customProxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyIp, proxyPort))
                proxyClient.newBuilder().proxy(customProxy).build()
            }

            val req = Request.Builder().url(url)
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", randomUserAgent())
                    .build()
            try {
                client.newCall(req).execute().use {
                    it.body().use {
                        return it?.string() ?: ""
                    }
                }
            } catch (e: Exception) {
//                logger.error("Get url={}, error={}", url, e.message)
            }

            if (retry > 0) {
                return get(url, proxyIp, proxyPort, retry - 1)
            }
            return ""
        }

        /**
         * 向指定URL发送POST方法的请求
         *
         * @param url 发送请求的URL
         * @param proxyIp 代理IP
         * @param proxyPort 代理端口
         * @param retry 重试次数
         * @return  所代表远程资源的响应结果
         */
        fun post(url: String, formBody: FormBody, proxyIp: String = LOCALHOST, proxyPort: Int = 0, retry: Int = 0): String {
            val client: OkHttpClient = if (proxyIp == LOCALHOST || proxyPort == 0) {
                commonClient
            } else {
                val customProxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyIp, proxyPort))
                proxyClient.newBuilder().proxy(customProxy).build()
            }

            val req = Request.Builder().post(formBody).url(url)
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", randomUserAgent())
                    .addHeader("Referer", url)
                    .build()
            try {
                client.newCall(req).execute().use { res ->
                    res.body().use {
                        return it?.string() ?: ""
                    }
                }
            }catch (e: SocketTimeoutException){
                // ignore
            } catch (e: Exception) {
//                logger.error("Post url={}", url, e)
            }

            if (retry > 0) {
                return get(url, proxyIp, proxyPort, retry - 1)
            }
            return ""
        }

        fun randomUserAgent(): String = USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.size)]
    }
}

//fun main(args: Array<String>) {
//    val formBody = FormBody.Builder()
//            .add("mid", "5")//设置参数名称和参数值
//            .build()
//    val html = HttpSender.post(BiliService.POST_MEMBER_API, formBody, "45.115.39.139", 7777)
//    println(html)
//}