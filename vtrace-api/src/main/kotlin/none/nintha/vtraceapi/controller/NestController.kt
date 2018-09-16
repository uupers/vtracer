package none.nintha.vtraceapi.controller

import io.swagger.annotations.Api
import none.nintha.vtraceapi.entity.Results
import none.nintha.vtraceapi.service.BiliService
import none.nintha.vtraceapi.spider.FetchTask
import none.nintha.vtraceapi.spider.Nest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.servlet.http.HttpServletResponse
import javax.validation.constraints.Null


@Api(tags = ["Nest"])
@RestController
@RequestMapping("api/v1")
class NestController {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    @Autowired
    lateinit var biliService: BiliService

    @GetMapping("/proxy/nest")
    fun proxySize(): Results<Map<String, Int>> {
        val map: Map<String, Int> = mapOf("total" to Nest.PROXY_MAP.size, "queue" to Nest.MAIN_QUEUE.size)
        return Results.success(map)
    }

    @GetMapping("/tasks/status")
    fun countTask(): Results<Map<String, Number>> {
        val map: Map<String, Number> = mapOf(
                "nestSize" to Nest.PROXY_MAP.size,
                "queue" to Nest.MAIN_QUEUE.size,
                "running" to BiliService.taskStatus,
                "total" to biliService.countTaskItem(),
                "finished" to biliService.countTaskResult())
        return Results.success(map)
    }

    fun configTask(): Results<Null> {
        return Results.success()
    }

    @PostMapping("/tasks/start")
    fun startTask(): Results<Null> {
        if (BiliService.taskStatus >= 1) return Results.failed("Task is running")

        biliService.runNormalAsync()
        return Results.success()
    }

    @PostMapping("/tasks/stop")
    fun stopTask(): Results<Null> {
        biliService.stop()
        while (true) {
            if (BiliService.taskStatus == 0) break

            Thread.sleep(100)
        }
        return Results.success()
    }

    @PostMapping("/tasks/upload")
    fun uploadTasks(file: MultipartFile): Results<Null> {
        if (BiliService.taskStatus != 0) {
            return Results.failed("Task is running")
        }
        val reader = file.inputStream.bufferedReader()
        biliService.resetTaskItem()
        Files.deleteIfExists(Paths.get(BiliService.zipFileName))
        println("uploadTasks start")
        val bufferSize = 5000
        val tasks: MutableList<FetchTask> = mutableListOf()
        while (true) {
            val line = reader.readLine() ?: break

            try {
                tasks.add(FetchTask(line.toLong()))
                if (tasks.size >= bufferSize) {
                    biliService.saveTaskItem(tasks)
                    tasks.clear()
                    println("uploadTasks $bufferSize")
                }
            } catch (e: Exception) {
                logger.warn(e.message)
            }
        }
        biliService.saveTaskItem(tasks)
        tasks.clear()
        println("uploadTasks done")
        return Results.success()
    }

    @PostMapping("/tasks/export")
    fun exportResult(): Results<Null> {
        biliService.exportTaskResult()
        return Results.success()
    }


    @GetMapping("/tasks/download")
    fun downloadResult(response: HttpServletResponse): Results<Null> {
        if (!Files.exists(Paths.get(BiliService.zipFileName))) {
            return Results.failed("file not found")
        }

        val downLoadPath = BiliService.zipFileName
        //获取文件的长度
        val fileLength = File(downLoadPath).length()

        //设置文件输出类型
        response.contentType = "application/octet-stream"
        response.setHeader("Content-disposition", "attachment; filename=${BiliService.zipFileName}")
        //设置输出长度
        response.setHeader("Content-Length", fileLength.toString())
        //获取输入流
        BufferedInputStream(FileInputStream(downLoadPath)).use { bis ->
            BufferedOutputStream(response.outputStream).use { bos ->
                val buff = ByteArray(2048)
                var bytesRead: Int
                while (true) {
                    bytesRead = bis.read(buff, 0, buff.size)
                    if (bytesRead == -1) break
                    bos.write(buff, 0, bytesRead)
                }
            }
        }
        return Results.success()
    }
}