package none.nintha.vtraceapi.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.mongodb.client.result.UpdateResult;
import none.nintha.vtraceapi.entity.*;
import none.nintha.vtraceapi.entity.consts.TraceStatus;
import none.nintha.vtraceapi.spider.TableNames;
import none.nintha.vtraceapi.util.HttpSender;
import none.nintha.vtraceapi.util.TimeUtil;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class TracerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    private final static String API_UP_SUBMIT = "http://space.bilibili.com/ajax/member/getSubmitVideos?mid=${mid}&pagesize=1&page=1&order=pubdate";
    private final static String API_VIDEO_STAT_BATCH = "http://api.bilibili.com/x/article/archives?ids=${aids}";
    private final static String API_VIDEO_STAT = "http://api.bilibili.com/x/web-interface/archive/stat?aid=${aid}";
    private final static String API_VIDEO_HTML = "http://www.bilibili.com/video/av${aid}";
    private final static String API_VIDEO_INFO = "http://api.bilibili.com/x/web-interface/view?aid=${aid}";

    // 默认视频追踪时长(天)
    private final static int TRACING_PERIOD_DAYS = 7;

    private final static int KEEP_MEMBER_MAX = 20;
    private final static int TRACE_MEMBER_MAX = 200;
    private final static int TRACE_VIDEO_MAX = 9999;
    private final static int TRACE_FAILURE_MAX = 5; // 失败次数上限，超过上限该视频停止追踪

    // {mid: aid}
    private final static Map<Long, Long> traceVideoMap = Maps.newConcurrentMap();
    // 失败计数器
    private final static Map<Long, AtomicInteger> failureCountMap = Maps.newConcurrentMap();
    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    BiliFetcher biliFetcher;

    @Autowired
    BiliService biliService;

    private static String retryGet(String url) {
        return HttpSender.Companion.get(url, HttpSender.LOCALHOST, 0, 3);
    }

    /**
     * 发现UP新上传视频
     *
     * @param mid
     */
    public void traceMemberArchive(long mid) {
        try {
            String html = retryGet(API_UP_SUBMIT.replace("${mid}", mid + ""));
            if (Strings.isBlank(html)) return;
            JsonNode node = mapper.readTree(html);
            JsonNode item = node.get("data").get("vlist").get(0);
            if (item == null) return;
            long aid = item.get("aid").asLong();

            Long lastAid = traceVideoMap.put(mid, aid);
            // 第一次运行或没有新视频结束任务
            if (lastAid == null || aid == lastAid) return;

            this.addTraceVideo(aid);
        } catch (Exception e) {
            logger.error("solve member least video error.", e);
        }
    }

    /**
     * 追踪UP数据变化，如fans
     *
     * @param mid
     */
    public void traceMemberInfo(long mid) {
        PartCard card = biliService.fetchCardLocally(mid);
        if (card.getMid() == 0L) return;

        long view = biliFetcher.fetchMemberArchiveView(mid);
        MemberInfo info = MemberInfo.ofPartCard(card);
        info.setArchiveView(view);
        MemberInfo lastMemberInfo = getLastMemberInfo(mid);
        // 重复数据不再插入
        if (Objects.equals(info, lastMemberInfo) == false) {
            mongoTemplate.insert(info, TableNames.MONGO_TRACE_MEMBER_INFO);
        }
    }

    /**
     * 获取最后一次用户信息追踪记录
     */
    private MemberInfo getLastMemberInfo(long mid) {
        Query query = new Query(Criteria.where("mid").is(mid)).with(new Sort(Sort.Direction.DESC, "ctime"));
        return mongoTemplate.findOne(query, MemberInfo.class, TableNames.MONGO_TRACE_MEMBER_INFO);
    }

    /**
     * 追踪视频数据
     *
     * @param aid
     */
    public void traceVideo(long aid) {
        try {
            String html = retryGet(API_VIDEO_STAT.replace("${aid}", aid + ""));
            if (Strings.isBlank(html)) return;
            JsonNode node = mapper.readTree(html);
            String dataJson = node.get("data").toString();
            VideoStat videoStat = mapper.readValue(dataJson, VideoStat.class);
            if (videoStat == null) return;

            VideoStat last = getLastVideoStat(aid);
            // 重复数据不再插入
            if (Objects.equals(videoStat, last) == false) {
                videoStat.setCtime(new Date());
                mongoTemplate.insert(videoStat, TableNames.MONGO_TRACE_VIDEO_STAT);
            }
        } catch (Exception e) {
            logger.error("solve video stat error, aid={}.", aid, e);
        }
    }

    /**
     * 批量追踪视频数据， 一次最多追踪100个
     */
    public void traceVideoBatch(List<Long> aidList) {
        if (CollectionUtils.isEmpty(aidList)) return;
        if (aidList.size() > 100) {
            aidList = aidList.subList(0, 100);
        }
        JsonNode node = null;
        try {
            String aids = aidList.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
            String html = retryGet(API_VIDEO_STAT_BATCH.replace("${aids}", aids));
            if (Strings.isBlank(html)) {
                logger.error("traceVideoBatch, fetch blank html, aids={}.", aidList);
                return;
            }
            node = mapper.readTree(html);
        } catch (Exception e) {
            logger.error("traceVideoBatch, fetch error, aids={}.", aidList, e);
        }
        for (long aid : aidList) {
            try {
                if (null == node.get("data").get(aid + "")) {
                    // 失败计数，连续超过N次停止追踪
                    AtomicInteger oldOne = failureCountMap.putIfAbsent(aid, new AtomicInteger(1));
                    int oldValue = oldOne == null ? 0 : oldOne.intValue();
                    logger.warn("traceVideoBatch, aid:{} is not exist, failure count={}", aid, oldValue + 1);
                    if (oldValue > 0) {
                        if (oldOne.incrementAndGet() > TRACE_FAILURE_MAX) {
                            this.updateTraceStatus(aid, TraceStatus.STOPPED.code);
                            failureCountMap.remove(aid);
                            logger.warn("Failure times is over limit, stop to trace aid:{}.", aid);
                        }
                    }
                    continue;
                }
                JsonNode jsonNode = node.get("data").get(aid + "").get("stat");
                if (jsonNode.isNull()) continue;

                VideoStat videoStat = mapper.readValue(jsonNode.toString(), VideoStat.class);
                if (videoStat == null) return;

                VideoStat last = getLastVideoStat(aid);
                // 重复数据不再插入
                if (Objects.equals(videoStat, last) == false) {
                    videoStat.setCtime(new Date());
                    mongoTemplate.insert(videoStat, TableNames.MONGO_TRACE_VIDEO_STAT);
                }
                // 成功追踪停止计数
                failureCountMap.remove(aid);
            } catch (Exception e) {
                logger.error("traceVideoBatch, loop error, aid={}.", aid, e);
            }
        }

    }

    private VideoStat getLastVideoStat(long aid) {
        Query query = new Query(Criteria.where("aid").is(aid)).with(new Sort(Sort.Direction.DESC, "ctime"));
        return mongoTemplate.findOne(query, VideoStat.class, TableNames.MONGO_TRACE_VIDEO_STAT);
    }

    public long countTraceVideoByMid(long mid) {
        return mongoTemplate.count(new Query(Criteria.where("mid").is(mid)), TableNames.MONGO_TRACE_VIDEO);
    }

    public List<TraceVideo> getTraceVideosByMid(long mid, long skip, int limit) {
        Sort sort2 = new Sort(Sort.Direction.DESC, "status", "ctime");
        Query query = new Query(Criteria.where("mid").is(mid)).with(sort2).limit(limit).skip(skip);
        return mongoTemplate.find(query, TraceVideo.class, TableNames.MONGO_TRACE_VIDEO);
    }

    public long countEnableTraceVideo() {
        return mongoTemplate.count(new Query(Criteria.where("status").is(TraceStatus.RUNNING)), TableNames.MONGO_TRACE_VIDEO);
    }

    public long countTraceVideo(QueryReq req) {
        return mongoTemplate.count(req.toQuery(), TableNames.MONGO_TRACE_VIDEO);
    }

    public List<TraceVideo> getTraceVideos(QueryReq req) {
        Sort sort = new Sort(Sort.Direction.DESC, "status", "ctime");
        Query query = req.toQuery().with(sort);
        return mongoTemplate.find(query, TraceVideo.class, TableNames.MONGO_TRACE_VIDEO);
    }

    public long countTraceMember(QueryReq req) {
        return mongoTemplate.count(req.toQuery(), TableNames.MONGO_TRACE_MEMBER);
    }

    public List<TraceMember> getTraceMembers(QueryReq req) {
        Sort sort = new Sort(Sort.Direction.DESC, "ctime");
        Query query = req.toQuery().with(sort);
        return mongoTemplate.find(query, TraceMember.class, TableNames.MONGO_TRACE_MEMBER);
    }

    /**
     * 获取所有 keep=1 的追踪用户
     */
    public List<TraceMember> getAllKeepTraceMembers() {
        QueryReq req = QueryReq.unpage();
        req.setKeep(1);
        return getTraceMembers(req);
    }

    public int addTraceMember(long mid) {
        TraceMember tm = mongoTemplate.findOne(new Query(Criteria.where("mid").is(mid)), TraceMember.class, TableNames.MONGO_TRACE_MEMBER);
        if (tm != null) return 0;
        if (this.countTraceMember(new QueryReq()) >= TRACE_MEMBER_MAX) return 0;

        PartCard card = biliService.fetchCardLocally(mid);
        if (card.getMid() == 0) return 0;
        TraceMember member = new TraceMember();
        member.setMid(mid);
        member.setName(card.getName());
        member.setFace(card.getFullFace());
        member.setCtime(new Date());
        mongoTemplate.insert(member, TableNames.MONGO_TRACE_MEMBER);
        return 1;
    }

    public int addTraceVideo(long aid) {
        boolean exists = mongoTemplate.exists(new Query(Criteria.where("aid").is(aid)), TableNames.MONGO_TRACE_VIDEO);
        if (exists) return 0;
        if (this.countEnableTraceVideo() >= TRACE_VIDEO_MAX) return 0;

        JsonNode videoInfo = fetchVideoInfo(aid);

        TraceVideo tv = new TraceVideo();
        tv.setAid(aid);
        tv.setMid(videoInfo == null ? 0 : videoInfo.get("owner").get("mid").asLong(0));
        tv.setStatus(TraceStatus.RUNNING.code);
        tv.setTitle(videoInfo == null ? Strings.EMPTY : videoInfo.get("title").asText());
        tv.setPic(videoInfo == null ? Strings.EMPTY : videoInfo.get("pic").asText());
        tv.setCtime(new Date());
        // 默认追踪7天
        tv.setEndTime(TimeUtil.toDate(LocalDateTime.now().plusDays(TRACING_PERIOD_DAYS)));
        // 把新视频加入追踪表里
        mongoTemplate.insert(tv, TableNames.MONGO_TRACE_VIDEO);
        return 1;
    }

    /**
     * 更新视频基本信息
     */
    public long updateTraceVideo(long aid) {
        JsonNode videoInfo = fetchVideoInfo(aid);
        if (videoInfo == null) {
            return 0;
        }

        Query query = new Query(Criteria.where("aid").is(aid));
        Update update = Update.update("pic", videoInfo.get("pic").asText(Strings.EMPTY))
                .set("title", videoInfo.get("title").asText(Strings.EMPTY))
                .set("mid", videoInfo.get("owner").get("mid").asLong(0));
        UpdateResult result = mongoTemplate.updateFirst(query, update, TableNames.MONGO_TRACE_VIDEO);
        return result.getModifiedCount();
    }

    private JsonNode fetchVideoInfo(long aid) {
        String html = null;
        try {
            html = retryGet(API_VIDEO_INFO.replace("${aid}", aid + ""));
            if (Strings.isBlank(html)) return null;

            TraceVideo traceVideo = new TraceVideo();
            JsonNode node = mapper.readTree(html);
            return node.get("data");
        } catch (Exception ex) {
            logger.error("fetch video info error.\n json:{}", html, ex);
        }
        return null;
    }

    public long updateEndTime(long aid, Date endTime) {
        Query query = new Query(Criteria.where("aid").is(aid));
        Update update = Update.update("endTime", endTime);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, TableNames.MONGO_TRACE_VIDEO);
        return updateResult.getModifiedCount();
    }

    public long updateTraceStatus(long aid, int status) {
        Query query = new Query(Criteria.where("aid").is(aid));
        Update update = Update.update("status", status);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, TableNames.MONGO_TRACE_VIDEO);
        return updateResult.getModifiedCount();
    }

    /**
     * 修改member的keep状态
     */
    public long updateMemberKeepStatus(long mid, int keep) {
        List<TraceMember> allKeepTraceMembers = this.getAllKeepTraceMembers();
        if (allKeepTraceMembers.size() >= KEEP_MEMBER_MAX) {
            return 0;
        }

        Query query = new Query(Criteria.where("mid").is(mid));
        Update update = Update.update("keep", keep);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, TableNames.MONGO_TRACE_MEMBER);
        return updateResult.getModifiedCount();
    }

    /**
     * 删除追踪member，同时删除对应的追踪数据
     */
    public long removeTraceMember(long mid) {
        Query query = new Query(Criteria.where("mid").is(mid));
        long effect = mongoTemplate.remove(query, TableNames.MONGO_TRACE_MEMBER).getDeletedCount();
        if (effect > 0) mongoTemplate.remove(query, TableNames.MONGO_TRACE_MEMBER_INFO);
        return effect;
    }

    /**
     * 删除追踪video，同时删除对应的追踪数据
     */
    public long removeTraceVideo(long aid) {
        Query query = new Query(Criteria.where("aid").is(aid));
        long effect = mongoTemplate.remove(query, TableNames.MONGO_TRACE_VIDEO).getDeletedCount();
        if (effect > 0) mongoTemplate.remove(query, TableNames.MONGO_TRACE_VIDEO_STAT);
        return effect;
    }

    /**
     */
    public List<VideoStat> getVideoStatForChart(long aid, Date startTime, Date endTime) {
        long interval = (endTime.getTime() - startTime.getTime()) / 300;
        TypedAggregation<VideoStat> agg = Aggregation.newAggregation(
                VideoStat.class,
                match(Criteria.where("aid").is(aid).and("ctime").gte(startTime).lte(endTime)),
                project("ctime", "coin", "share", "view", "danmaku", "favorite", "reply", "like", "dislike")
                        .andExpression("ceil((ctime - [0]) / [1])", new Date(0), interval)
                        .as("cdate"),
                group("cdate")
                        .max("view").as("view")
                        .max("coin").as("coin")
                        .max("danmaku").as("danmaku")
                        .max("favorite").as("favorite")
                        .max("reply").as("reply")
                        .max("share").as("share")
                        .max("like").as("like")
                        .max("dislike").as("dislike")
                        .max("ctime").as("ctime"),
                sort(Sort.Direction.ASC, "ctime"));
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, TableNames.MONGO_TRACE_VIDEO_STAT, Map.class);
        List<Map> mappedResults = results.getMappedResults();
        return mappedResults.stream().map(v -> mapper.convertValue(v, VideoStat.class)).collect(Collectors.toList());
    }

    /**
     */
    public List<MemberInfo> getMemberInfoForChart(long mid, Date startTime, Date endTime) {
        long interval = (endTime.getTime() - startTime.getTime()) / 300;
        TypedAggregation<MemberInfo> agg = Aggregation.newAggregation(
                MemberInfo.class,
                match(Criteria.where("mid").is(mid).and("ctime").gte(startTime).lte(endTime)),
                project("ctime", "fans", "archiveView")
                        .andExpression("ceil((ctime - [0]) / [1])", new Date(0), interval)
                        .as("cdate"),
                group("cdate")
                        .max("fans").as("fans")
                        .max("archiveView").as("archiveView")
                        .max("ctime").as("ctime"),
                sort(Sort.Direction.ASC, "ctime"));
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, TableNames.MONGO_TRACE_MEMBER_INFO, Map.class);
        List<Map> mappedResults = results.getMappedResults();
        // 先用map接收数据可以过滤NULL值，防止NPE异常
        return mappedResults.stream().map(v -> mapper.convertValue(v, MemberInfo.class)).collect(Collectors.toList());
    }

//    public static void main(String[] args) {
////        new TracerService().traceVideoBatch(Arrays.asList(24852251L, 24834131L, 24833129L, 24823510L, 24788948L, 24750208L, 24729595L, 24706532L, 24702428L, 24700430L, 24669669L, 24664229L, 24633291L, 2580484L, 24618353L, 24586349L, 24577001L, 24575061L, 24561131L, 24525126L, 24511060L, 24507374L, 24503002L, 24500861L, 24500598L, 24165605L, 23937952L, 23867003L));
//        JsonNode jsonNode = new TracerService().fetchVideoInfo(31509186L);
//        System.out.println(jsonNode);
//    }

}
