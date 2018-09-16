package none.nintha.vtraceapi.controller

import io.swagger.annotations.Api
import none.nintha.vtraceapi.entity.QueryReq
import none.nintha.vtraceapi.entity.Results
import none.nintha.vtraceapi.entity.consts.TraceStatus
import none.nintha.vtraceapi.service.TracerService
import none.nintha.vtraceapi.service.TracerTask
import none.nintha.vtraceapi.util.TimeUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import javax.validation.constraints.Null

@Api(tags=["Trace"])
@RestController
@RequestMapping("api/v1")
class TraceController {
    @Autowired
    lateinit var tracerService: TracerService

    @Autowired
    lateinit var tracerTask: TracerTask

    @PostMapping("/trace/member/{mid}")
    fun addTraceMember(@PathVariable mid: Long): Results<Map<String, Int>> {
        val effect = tracerService.addTraceMember(mid)
        return Results.success(mapOf("effect" to effect))
    }

    @PostMapping("/trace/video/{aid}")
    fun addTraceVideo(@PathVariable aid: Long): Any {
        val effect = tracerService.addTraceVideo(aid)
        return Results.success(mapOf("effect" to effect))
    }

    @GetMapping("/trace/member")
    fun listTraceMember(req: QueryReq): Any {
        val count = tracerService.countTraceMember(req)
        val members = tracerService.getTraceMembers(req)
        return Results.success(mapOf("list" to members, "total" to count, "pageSize" to req.pageSize))
    }

    @GetMapping("/trace/video")
    fun listTraceVideo(req: QueryReq): Any {
        val count = tracerService.countTraceVideo(req)
        val list = tracerService.getTraceVideos(req)
        return Results.success(mapOf("list" to list, "total" to count, "pageSize" to req.pageSize))
    }

    @GetMapping("/trace/video/mid/{mid}/page/{pageNum}")
    fun listTraceVideoByMid(
            @PathVariable mid: Long,
            @PathVariable pageNum: Int,
            @RequestParam(defaultValue = "20") pageSize: Int): Any {

        val count = tracerService.countTraceVideoByMid(mid)
        val list = tracerService.getTraceVideosByMid(mid, (pageNum - 1L) * pageSize, pageSize)
        return Results.success(mapOf("list" to list, "total" to count, "pageSize" to pageSize))
    }

    @GetMapping("/trace/videoStat/{aid}")
    fun listVideoStatForChart(@PathVariable aid: Long, st: Long, et: Long): Results<*> {
        //        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        val sTime = Date.from(Instant.ofEpochMilli(st))
        val eTime = Date.from(Instant.ofEpochMilli(et))
        val list = tracerService.getVideoStatForChart(aid, sTime, eTime)
        return Results.success(mapOf("list" to list, "total" to list.size))
    }

    @GetMapping("/trace/memberInfo/{mid}")
    fun listMemberInfoForChart(@PathVariable mid: Long, st: Long, et: Long): Any {
        //        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        val sTime = Date.from(Instant.ofEpochMilli(st))
        val eTime = Date.from(Instant.ofEpochMilli(et))
        val list = tracerService.getMemberInfoForChart(mid, sTime, eTime)
        return Results.success(mapOf("list" to list, "total" to list.size))
    }

    @PutMapping("/trace/video/{aid}/status/{status}")
    fun updateTraceStatus(@PathVariable aid: Long, @PathVariable status: Int): Any {
        val effect = tracerService.updateTraceStatus(aid, status)
        // 开启后自动续期一周
        if (effect > 0 && status == TraceStatus.RUNNING.code) {
            val weekAfter = LocalDateTime.now().plusDays(7)
            tracerService.updateEndTime(aid, TimeUtil.toDate(weekAfter))
        }
        return Results.success(mapOf("effect" to effect))
    }

    @PutMapping("/trace/member/{mid}/keep/{keep}")
    fun updateMemberKeep(@PathVariable mid: Long, @PathVariable keep: Int): Any {
        val effect = tracerService.updateMemberKeepStatus(mid, keep)
        return Results.success(mapOf("effect" to effect))
    }

    @DeleteMapping("/trace/member/{mid}")
    fun removeTraceMember(@PathVariable mid: Long): Any {
        val effect = tracerService.removeTraceMember(mid)
        return Results.success(mapOf("effect" to effect))
    }

    //  测试用户爬取功能
    @GetMapping("/test/task/member")
    fun testTraceMember(): Results<Null> {
        tracerTask.memberLoop()
        return Results.success()
    }

    //  测试视频爬取功能
    @GetMapping("/test/task/video")
    fun testTraceVideo(): Results<Null> {
        tracerTask.videoLoop()
        return Results.success()
    }
}