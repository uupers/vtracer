package none.nintha.vtraceapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.WriteConcern
import none.nintha.vtraceapi.config.asBean
import none.nintha.vtraceapi.entity.Member
import none.nintha.vtraceapi.entity.PartCard
import none.nintha.vtraceapi.entity.consts.TaskMode
import none.nintha.vtraceapi.spider.FetchTask
import none.nintha.vtraceapi.spider.Nest
import none.nintha.vtraceapi.spider.Spider
import none.nintha.vtraceapi.spider.TableNames
import none.nintha.vtraceapi.util.HttpSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@Service
class BiliService {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BiliService::class.java)
        const val POST_MEMBER_API = "http://space.bilibili.com/ajax/member/GetInfo"
        const val GET_CARD_API = "http://api.bilibili.com/x/web-interface/card" // ?mid={mid}
        val sexMap: Map<String, Int> = mapOf("男" to 0, "女" to 1, "保密" to 2)
        var taskStatus: Int = 0 // 0-stopped, 1-running, 2-exporting, 3-stopping, 4-scheduled
        var taskThread: Thread? = null
        var taskMode: TaskMode = TaskMode.IN_MEMORY

        const val apiType = 0 // 0-GET:PartCard, 1-POST:Member
        const val zipFileName = "result.zip"
        const val csvFileName = "result.csv"
        var defaultTaskId: Long = 0
    }

    @Autowired
    lateinit var mongoTemplate: MongoTemplate
    @Autowired
    lateinit var mapper: ObjectMapper
    @Value("@{spider.task-package-size:1000}")
    var packageSize: Int = 1000
    @Value("@{mongo.export-command}")
    lateinit var exportCommand: String

    fun stop() {
        logger.info("[Run task] try to stop")
        if (taskStatus == 1) taskStatus = 3
    }

    fun runNormalAsync() {
        if(taskMode != TaskMode.NORMAL){
            logger.warn("[Run task] mode not match, current mode is $taskMode")
            return
        }
        taskThread = Thread(this::runNormalTask).also { it.start() }
    }

    fun runInMemoryTask() {
        if(taskMode != TaskMode.IN_MEMORY){
            logger.warn("[Run task] mode not match, current mode is $taskMode")
            return
        }
        defaultTaskId = Date().time
        logger.info("[Run task In Memory] start.")
        val tasks = mongoTemplate.findAll(FetchTask::class.java, TableNames.MONGO_TRACE_TASK_TIEM)
        var remainMids = tasks.asSequence().map { it.mid }.toMutableSet()
        while (true) {
            val finishedMids = handleTask(remainMids.map { FetchTask(it) }, this::fetchCard, PartCard::mid)

            logger.info("[Run task In Memory] ${finishedMids.size}/${remainMids.size}")
            remainMids.removeAll(finishedMids)
            if (remainMids.isEmpty()) break
        }
        logger.info("[Run task In Memory] done.")
    }

    private fun runNormalTask() {
        if (taskStatus >= 1) return

        logger.info("[Run task] start.")
        taskStatus = 1
        defaultTaskId = 0
        while (taskStatus == 1) {
            val tasks = getTaskPackage(packageSize)
            if (CollectionUtils.isEmpty(tasks)) break
            handleTaskByApiType(tasks, apiType)
        }
        if (taskStatus == 3) logger.info("[Run task] interrupted.")
        else logger.info("[Run task] done.")
        taskStatus = 0

        if (this.countTaskItem() == this.countTaskResult() && Files.exists(Paths.get(zipFileName))) {
            this.exportTaskResult()
        }
    }

    fun handleTaskByApiType(tasks: List<FetchTask>, apiType: Int) {
        val st = System.currentTimeMillis()
        val finishedSize = when (apiType) {
            0 -> handleTask(tasks, this::fetchCard, PartCard::mid)
            1 -> handleTask(tasks, this::fetchMember, Member::mid)
            else -> throw RuntimeException("未定义的API Type")
        }.size
        val et = System.currentTimeMillis()
        logger.info("[Package] $finishedSize/${tasks.size}, ${finishedSize * 1000 / (et - st)}/s")
    }

    /**
     * 任务代理，根据传入的参数执行任务
     * @param tasks 任务内容
     * @param fetchFunc 爬取方法
     * @param filterFunc 过滤结果，对成功爬取的任务返回True
     * @param saveFunc 把结果保存在数据库
     * @return 成功爬取的mid
     */
    fun <T> handleTask(tasks: List<FetchTask>, fetchFunc: (Long) -> T, idMapper: (T) -> Long): List<Long> {
        return tasks.map { task ->
            CompletableFuture.supplyAsync(Supplier { fetchFunc(task.mid) }, HttpSender.threadPool)
        }.asSequence()
                .map { it.get() }
                .filter { idMapper.invoke(it) > 0 }
                .toList()
                .apply { saveTaskResult(this, idMapper) }
                .map(idMapper)
    }

    fun fetchMember(mid: Long): Member {
        val spider = Nest.getSpider()
        val html = spider.fetchMember(mid).also { Nest.returnSpider(spider) }
        if (html.isBlank()) return Member(0)
        try {
            val res = mapper.readTree(html)
            val status = res.get("status")?.asBoolean() ?: false
            if (status) {
                val data: JsonNode = res.get("data")
                val member = data.asBean(Member::class.java, mapper)
                member.sex = sexMap[data.get("sex")?.asText()] ?: 2
                member.face = member.face.split("face/").run {
                    if (this.size == 2) this[1] else member.face
                }
                member.level = data.get("level_info")?.get("current_level")?.asInt() ?: 0
                return member
            } else {
                return Member(mid)
            }
        } catch (e: Exception) {
            logger.debug("[Fetch Member] error mid=$mid, none.nintha.vtraceapi.spider=${spider.getProxyAddress()}(FT=${spider.failTimes}), html=$html", e)
        }
        return Member(0)
    }

//    fun saveMember(members: Collection<Member>) {
//        if (CollectionUtils.isEmpty(members)) return
//        mongoTemplate.setWriteConcern(WriteConcern.UNACKNOWLEDGED)
//        mongoTemplate.insert(members, TableNames.MONGO_MEMBER_INFO);
//        members.map { it.mid }.also { finishTask(it) }
//    }
    fun fetchCardLocally(mid: Long): PartCard{
        return fetchCardCommon(mid, Spider(HttpSender.LOCALHOST,0))
    }
    fun fetchCard(mid: Long): PartCard{
        return fetchCardCommon(mid, null)
    }

    fun fetchCardCommon(mid: Long, theSpider: Spider? = null): PartCard {
        val spider = theSpider ?: Nest.getSpider()
        val html = spider.fetchCard(mid).also { Nest.returnSpider(spider) }
        if (html.isBlank()) return PartCard(0)
        try {
            val res = mapper.readTree(html)
            val code = res.get("code")?.asInt() ?: -1
            return if (code == 0) {
                val partCard = PartCard(mid)
                res.get("data").get("card").apply {
                    partCard.name = this.get("name").asText()
                    partCard.fans = this.get("fans").asLong()
                    partCard.attention = this.get("attention").asLong()
                    partCard.face = this.get("face").asText().run {
                        if (this.contains("face/")) this.split("face/")[1] else this
                    }
                    partCard.sex = sexMap[this.get("sex").asText()] ?: 2
                    partCard.sign = this.get("sign").asText().trim()
                    partCard.rtime = Date().time
                    partCard.taskId = defaultTaskId
                }
                partCard.archive = res.get("data").get("archive_count").asLong()
                partCard
            } else {
                PartCard(mid)
            }
        } catch (e: Exception) {
            logger.debug("[Fetch Card] error mid=$mid, none.nintha.vtraceapi.spider=${spider.getProxyAddress()}(FT=${spider.failTimes}), html=$html", e)
        }
        return PartCard(0)
    }

//    fun saveCard(cards: Collection<PartCard>) {
//        if (CollectionUtils.isEmpty(cards)) return
//        mongoTemplate.setWriteConcern(WriteConcern.UNACKNOWLEDGED)
//        mongoTemplate.insert(cards, TableNames.MONGO_PART_CARD);
//        cards.map { it.mid }.also { finishTask(it) }
//    }

    fun <T> saveTaskResult(results: Collection<T>, idMapper: (T) -> Long) {
        if (CollectionUtils.isEmpty(results)) return
        mongoTemplate.setWriteConcern(WriteConcern.UNACKNOWLEDGED)
        mongoTemplate.insert(results, TableNames.MONGO_TRACE_TASK_RESULT)
        results.map(idMapper).also {
            val query = Query(Criteria.where("mid").`in`(it))
            val update = Update.update("status", FetchTask.STATUS_FINISHED)
            mongoTemplate.updateMulti(query, update, TableNames.MONGO_TRACE_TASK_TIEM)
        }
    }

    fun resetTaskResult() {
        mongoTemplate.dropCollection(TableNames.MONGO_TRACE_TASK_RESULT)
        val index = Index().on("mid", Sort.Direction.ASC).on("taskId", Sort.Direction.DESC).unique()
        mongoTemplate.indexOps(TableNames.MONGO_TRACE_TASK_RESULT).ensureIndex(index)
    }

    fun resetTaskItem() {
        mongoTemplate.dropCollection(TableNames.MONGO_TRACE_TASK_TIEM)
        mongoTemplate.indexOps(TableNames.MONGO_TRACE_TASK_TIEM).ensureIndex(Index("mid", Sort.Direction.ASC).unique())
        mongoTemplate.indexOps(TableNames.MONGO_TRACE_TASK_TIEM).ensureIndex(Index("status", Sort.Direction.ASC))
    }

    fun saveTaskItem(items: Collection<FetchTask>) {
        mongoTemplate.setWriteConcern(WriteConcern.UNACKNOWLEDGED)
        mongoTemplate.insert(items, TableNames.MONGO_TRACE_TASK_TIEM)
    }

    fun getTaskPackage(packageSize: Int): List<FetchTask> {
        val criteria = Criteria.where("status").lte(FetchTask.STATUS_WAITING)
        val query = Query(criteria).limit(packageSize)
        val tasks: MutableList<FetchTask> = mongoTemplate.find(query, FetchTask::class.java, TableNames.MONGO_TRACE_TASK_TIEM)
        return tasks
    }

//    fun finishTask(mids: List<Long>) {
//        val query = Query(Criteria.where("mid").`in`(mids))
//        val update = Update.update("status", FetchTask.STATUS_FINISHED)
//        mongoTemplate.updateMulti(query, update, TableNames.MONGO_FETCH_TASK)
//    }

    fun configTask() {

    }

    fun countTaskItem(): Long {
        return mongoTemplate.count(Query(), TableNames.MONGO_TRACE_TASK_TIEM)
    }

    fun countTaskResult(): Long {
        return mongoTemplate.count(Query(), TableNames.MONGO_TRACE_TASK_RESULT)
    }

    fun exportTaskResult() {
        if (taskStatus != 0) return

        logger.info("[Export] start")
        taskStatus = 2
        Files.deleteIfExists(Paths.get(zipFileName))
        val process: Process = Runtime.getRuntime().exec(exportCommand)
        val bufferedReader = process.errorStream.bufferedReader()
        while (true) {
            val line = bufferedReader.readLine() ?: break
            println(line)
        }
        logger.info("[Export] csv -> zip")
        val fis = Files.newInputStream(Paths.get(csvFileName))
        val reader = fis.bufferedReader()
        val fos = Files.newOutputStream(Paths.get(zipFileName))

        ZipOutputStream(fos).apply { this.setLevel(9) }.use { zip ->
            zip.putNextEntry(ZipEntry("output.csv"))
            while (true) {
                val line = reader.readLine() ?: break
                zip.write("$line\n".toByteArray())
            }
            zip.closeEntry()
        }
        Files.deleteIfExists(Paths.get(csvFileName))
        taskStatus = 0
        logger.info("[Export] done")
    }
}