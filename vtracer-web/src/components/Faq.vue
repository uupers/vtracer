<template>
    <div>
        <h1 style="margin-left:50px">FAQ</h1>
        <pre>
            Q: 这个网站是干嘛的？
            A: 本站提供对Bilibili弹幕网中部分UP及其视频进行信息追踪功能。
            默认情况下，对UP追踪粉丝数和总播放量；对视频追踪播放量、硬币、分享数、弹幕数、收藏数、评论数、赞、踩；
            其中视频信息追踪默认持续7天（具体时间以EndTime属性为准），7天后视频的“Enable”属性会变成关闭状态，你可以手动开启让它继续追踪。

            Q: UP列表中的Keep On是什么？
            A: 开启Keep On后，对应UP下的所有视频会无视7天限制（即无视EndTime属性），长期追踪。

            Q: Litte Nest是干啥的？
            A: 半自动化的简易代理爬虫，用户上传一个csv，里面是需要爬的mid，然后这个功能会使用代理IP去爬对应的用户信息，
            任务完成后，可以点击下载爬取的数据（CSV压缩包）

            Q: 怎么注册和登录？
            A: 目前不开放注册，需要账号的话可以加QQ群：722576063。

            Q: 网站已经半天没有新的跟踪数据了怎么办/网站好像卡住了？
            A: 由于VPS性能有限，的确会出现卡顿/追踪停止的情况，可以在群里大喊“vtracer又挂了！”。

            Q: 我想提Bug/建议/改进/新功能。
            A: https://github.com/nintha/vtracer
        </pre>

    </div>
</template>

<script>
export default {};
</script>