<template>
  <div>
    <h3>Nest Info</h3>
    <Progress :percent="spiderTotalNum == 0 ? 0:spiderBusyNum * 100 / spiderTotalNum" status="active" :stroke-width="20">
      <span style="display:inline-block; width:200px;">{{spiderBusyNum}} / {{spiderTotalNum}}</span>
    </Progress>
    <br><br>
    <h3>Task Process: {{getProcess()}}</h3>
    <Progress :percent="taskTotalNum == 0 ? 0:taskFinishedNum * 100 / taskTotalNum" status="active" :stroke-width="20">
      <span style="display:inline-block; width:200px;">{{taskFinishedNum}} / {{taskTotalNum}}</span>
    </Progress>
    <br><br>
    <div style="text-align:right; padding: 0 40px;">
      <!-- <Button v-if="waitting" type="dashed">{{waittingMessage}}</Button> -->
      <!-- <Button @click="startTask" v-if="!waitting && running === 0" type="primary" ghost>Start Task</Button> -->
      <Button @click="stopTask" v-if="!waitting && running === 1" type="warning" ghost>Stop Task</Button>
      <Button @click="openDrawer" v-if="!waitting && running === 0" type="error" ghost>Upload CSV</Button>
      <!-- <Button @click="downloadFile" v-if="!waitting && running === 0 && taskTotalNum === taskFinishedNum && taskTotalNum > 0" type="primary" ghost>Download</Button> -->
    </div>
    <Divider dashed />
    <h3>Tips</h3>
    <pre>
      Nest Info： 蜘蛛巢信息，下方的数字分别表示 忙碌的蜘蛛 和 总共的蜘蛛
      Task Process： 任务进度，下方的数字分别表示 已完成任务数 和 总任务数
      
      按钮：
        Upload CSV：上传新的任务文件，该操作会覆盖前一个任务
    </pre>

    <h3>API Info</h3>
    <pre>
      API: http://api.bilibili.com/x/web-interface/card?mid={mid}
      
      Data:
        {
          "mid": "123",//UP的用户ID
          "archive": 2,//上传过的稿件数量
          "attention": 7,//关注的用户数量
          "face": "34c5b30a990c7ce4a809626d8153fa7895ec7b63.gif",//头像，使用时需要加上"http://i2.hdslb.com/bfs/face/"前缀
          "fans": 16866,//粉丝数
          "name": "bishi",//用户名
          "sex": 0,//性别 0-男,1-女,2-保密
          "sign": ""//个性签名
        }
    </pre>

    
    <Drawer title="Upload CSV" :closable="false"  v-model="inReset" :mask-closable="false">
      <h2>重置任务</h2>
      <hr>
      <p> 上传新的任务文件，该操作会覆盖前一个任务</p>
      <p> CSV文件，文件每一行一个mid</p>
      <br><br>
      <Upload 
          ref="upload"
          :on-success="onUploadSuccess"
          :on-error="onUploadError"
          :on-exceeded-size="handleMaxSize"
          type="drag"
          :max-size="1024"
          action="/api/v1/tasks/upload">
          <div style="padding: 20px 0">
            <Icon type="ios-cloud-upload" size="52" style="color: #3399ff"></Icon>
            <p>Click or drag files here to upload</p>
        </div>
      </Upload>
      <br>
      <hr>
      <div style="text-align: right; padding: 5px;"><Button style="margin-right: 8px" @click="inReset = false; syncStatus()">Close</Button></div>
    </Drawer>
  </div>
</template>

<script>
import moment from "moment";
export default {
  components: {},
  data() {
    return {
      waittingMessage: "In Sync",
      waitting: true,
      inReset: false,
      running: 0,
      spiderTotalNum: 0,
      spiderFreeNum: 0,
      spiderBusyNum: 0,
      taskTotalNum: 0,
      taskFinishedNum: 0,
      timerId: 0
    };
  },
  methods: {
    getProcess(){
      return ["已停止","运行中","生成CSV文件","正在停止"][this.running]
    },
    getTaskStatus() {
      this.$api.get("/tasks/status", {}, r => {
        this.running = r.data.running;
        this.taskTotalNum = r.data.total;
        this.taskFinishedNum = r.data.finished;
        this.spiderTotalNum = r.data.nestSize;
        this.spiderFreeNum = r.data.queue;
        this.spiderBusyNum = this.spiderTotalNum - this.spiderFreeNum;

        switch(this.running){
          case 0:
          case 1:
            this.waitting = false
          break
          case 2:
            this.waitting = true
            this.waittingMessage = 'Creating CSV'
          break
          case 3:
            this.waitting = true
            this.waittingMessage = 'Stopping'
          break
        }
      });
    },
    // getNestStatus() {
    //   this.$api.get("/proxy/nest", {}, r => {
    //     this.spiderTotalNum = r.data.total;
    //     this.spiderFreeNum = r.data.queue;
    //     this.spiderBusyNum = this.spiderTotalNum - this.spiderFreeNum;
    //   });
    // },
    syncStatus(){
        this.getTaskStatus();
        // this.getNestStatus();
    },
    startTask() {
      this.$api.post("/tasks/start", {}, r => {
        this.getTaskStatus();
      });
    },
    stopTask() {
      this.waitting = true
      this.waittingMessage = 'Stopping'
      this.$api.post("/tasks/stop", {}, r => {
        this.getTaskStatus();
      });
    },
    configTimer() {
      clearInterval(this.timerId);
      this.timerId = setInterval(() => {
        if (this.running === 0) return;

        this.syncStatus()
      }, 4000);
    },
    onUploadSuccess(event, file, fileList) {
      if(event.code === 0){
        this.$Message.success('upload file success');
      }else{
        this.$Message.error(event.message);
      }
    },
    onUploadError(event, file, fileList){
      this.$Message.error(event);
    },
    handleMaxSize(){
      this.$Message.error("max file size is 1MB");
    },
    clearUploadedFiles () {
      this.$refs.upload.clearFiles();
    },
    openDrawer(){
      this.clearUploadedFiles()
      this.inReset = true
    },
    downloadFile(){
      window.open(`${this.getApiRoot()}/tasks/download`)
    },
    getApiRoot(){
      return this.$api.root
    }
  },
  created() {
    this.syncStatus()
    this.configTimer();
  },
  beforeDestroy() {
    clearInterval(this.timerId);
  }
};
</script>
