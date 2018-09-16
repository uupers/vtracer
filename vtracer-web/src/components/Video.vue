<template>
  <div>
      <transition name="slide-fade">
        <div v-if="!isVideoStat" key="video">
          <Row>
              <Col span="20"> 
                <Input v-model="title" placeholder="视频名称/aid" style="width: 300px">
                    <Button slot="append" icon="ios-search"  @click="filterSearch"></Button>
                </Input>
              </Col>
              <Col span="4"><Button type="primary" @click="renderModal">Trace new video</Button></Col>
          </Row>
          <br/>
          <Table stripe :loading="loading" :columns="columns" :data="data"></Table>
          <br>
          <Page :total="total" :page-size="pageSize" :current="page" show-total class="paging" @on-change="fetchData"></Page>
          <br>
        </div>
        <div v-else key="stat">
          <Button type="primary" @click="isVideoStat=false">Back</Button>
          <br/> <br/>
          <VideoStat :record="record"></VideoStat>
        </div>
      </transition>
  </div>
</template>

<script>
import VideoStat from './VideoStat.vue'
import moment from 'moment'
export default {
  components: { VideoStat },
  data() {
    return {
      isVideoStat: false,
      record: {},
      loading: true,
      columns: [
        {
          title: 'Title',
          key: 'title',
          width: 450,
          render: (h, data) => {
            return h('div', [
              h('img', {
                attrs: { src: data.row.pic, height: '50px', width: '80px' },
                style: { verticalAlign: 'middle', marginRight: '5px', cursor: 'pointer' },
                on: {
                  click: () => window.open(`https://www.bilibili.com/video/av${data.row.aid}`, '_blank')
                }
              }),
              h('span', {style: { verticalAlign: 'middle', display:'inline-block', width:'320px'}}, data.row.title)
            ])
          }
        },
        {
          title: 'Record Time',
          key: 'ctime',
          render: (h, data) => h('span', moment(data.row.ctime).format('YYYY-MM-DD HH:mm:ss'))
        },
        {
          title: 'End Time',
          key: 'endTime',
          render: (h, data) => h('span', moment(data.row.endTime).format('YYYY-MM-DD HH:mm:ss'))
        },
        {
          title: 'Tracing',
          key: 'status',
          render: (h, data) => {
            return h('i-switch', {
              props: {
                type: 'primary',
                value: data.row.status === 1 //控制开关的打开或关闭状态，官网文档属性是value
              },
              on: {
                //触发事件是on-change,用引号括起来
                'on-change': value =>
                  this.toggleTraceStatus(data.row.aid, value ? 1 : 0, data.row.title)
              }
            },[
              h('Icon',{slot:'open', props:{type:'android-done'}}),
              h('Icon',{slot:'close', props:{type:'android-close'}}),
            ])
          }
        },
        {
          title: 'Action',
          key: 'action',
          align: 'center',
          render: (h, params) => {
            return h('div', [
              h(
                'Button',
                {
                  props: {
                    type: 'primary',
                    size: 'small'
                  },
                  style: {
                    marginRight: '5px'
                  },
                  on: {
                    click: () => this.viewStat(params.row)
                  }
                },
                'View'
              )
            ])
          }
        }
      ],
      data: [],
      total: 0,
      page:1,
      pageSize: 20,
      title: ''
    }
  },
  methods: {
    filterSearch(){
      this.fetchData();
    },
    fetchData(page = 1) {
      this.page = page
      this.$api.get(`/trace/video`, {pageNum:page,title:this.title}, r => {
        this.data = r.data.list
        this.total = r.data.total
        this.pageSize = r.data.pageSize
        this.loading = false
      })
    },
    toggleTraceStatus(aid, status, title) {
      this.$api.put(`/trace/video/${aid}/status/${status}`, {}, r => {
        if (r.data.effect > 0) {
          // this.fetchData()
          // this.$Notice.success({
          //   desc: title,
          //   title: status > 0 ? `Tracing is running.` : `Tracing is stopped.`
          // })
        } else {
          this.$Notice.error({
            title: `Oops! ${title} is out of control.`
          })
        }
      })
    },
    renderModal() {
      const vee = this
      let tempValue
      this.$Modal.confirm({
        render: h => {
          return h('Input', {
            props: {
              autofocus: true,
              placeholder: 'Please enter aid'
            },
            on: {
              input: val => (tempValue = val)
            }
          })
        },
        onOk() {
          vee.addVideo(tempValue)
        }
      })
    },
    addVideo(aid) {
      if (Number.isInteger(Number(aid)) == false) {
        this.$Notice.error({
          title: `Oops! "${aid}" is invalid aid`
        })
        return
      }
      this.$api.post(`/trace/video/${aid}`, {}, r => {
        if (r.data.effect > 0) {
          this.fetchData()
          this.$Notice.success({
            title: `Great! we are tracing ${aid}`
          })
        } else {
          this.$Notice.error({
            title: `Oops! ${aid} is dups or invalid`
          })
        }
      })
    },
    viewStat(record) {
      this.record = record
      this.isVideoStat = true
    },
  },
  created() {
    this.fetchData()
  }
}
</script>
