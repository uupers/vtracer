package none.nintha.vtraceapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import none.nintha.vtraceapi.entity.consts.ApiGen
import none.nintha.vtraceapi.util.HttpSender
import org.apache.logging.log4j.util.Strings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class BiliFetcher {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BiliService::class.java)
    }
    @Autowired
    lateinit var mongoTemplate: MongoTemplate
    @Autowired
    lateinit var mapper: ObjectMapper
    @Value("@{spider.task-package-size:1000}")
    var packageSize: Int = 1000

    fun fetchMemberArchiveView(mid: Long): Long {
        try {
            val html = HttpSender.get(ApiGen.memberTotalView(mid))
            if (Strings.isBlank(html)) return 0

            val node = mapper.readTree(html)
            return node.get("data").get("archive").get("view").asLong()
        } catch (e: IOException) {
            logger.error("解析用户总播放量异常", e)
        }
        return 0
    }
}
