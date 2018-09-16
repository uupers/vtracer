<template>
  <div>
    <!-- <div :style="{float:'right', marginRight:'50px'}">
      <DatePicker type="datetimerange" 
                placeholder="Select date and time" 
                style="width: 300px"
                :value="timestamp2str(dateValue)"
                :disabled="true"
                @on-change="fetchVideoStat"></DatePicker>
    </div> -->
    <h2>{{record.title}}</h2>
    <div :style="{position:'relative'}">            
      <LineChart :opt="opt"></LineChart>
      <Spin fix v-if="spinShow">
        <Icon type="load-c" size=18 class="demo-spin-icon-load"></Icon>
        <div>Loading</div>
      </Spin>
    </div>
    <br>
    <DateSlider :startTime="dateValue[0]" :endTime="dateValue[1]" :onchange="sliderChanged"></DateSlider>
  </div>
</template>
<style>
.demo-spin-icon-load {
  animation: ani-demo-spin 1s linear infinite;
}
</style>
<script>
import LineChart from './LineChart.vue'
import DateSlider from './DateSlider.vue'
import moment from 'moment'
import Vue from 'vue'

export default {
  components: { LineChart, DateSlider },
  props: {
    record: Object
  },
  data() {
    const et = moment().isBefore(moment(this.record.endTime))
      ? +moment()
      : this.record.endTime
    return {
      spinShow: true,
      dateValue: [this.record.ctime, et],
      opt: {}
    }
  },
  mounted() {
    this.fetchVideoStat(this.dateValue)
  },
  methods: {
    fetchVideoStat(timerange) {
      // this.rangeFetch(timerange)
      this.spinShow = true
      const url = `/trace/videoStat/${this.record.aid}`
      this.$api.get(url, { st: timerange[0], et: timerange[1] }, r => {
        if (r.data.list) {
          this.opt = this.createOpt(r.data.list)
          this.spinShow = false
        }
      })
    },
    createOpt(videoStatList) {
      const fields = ['view', 'coin', 'share', 'danmaku', 'favorite', 'reply', 'like', 'dislike']
      let obj = {
        tooltip: {
          trigger: 'axis',
          formatter: v => {
            let str = `${moment(v[0].value[0]).format('YYYY-MM-DD HH:mm:ss')}`
            for(let i=0; i < v.length; i++){
              str += `<br/>${v[i].marker} ${v[i].name}: ${v[i].value[1]}`
            }
            return str
          }
        },
        legend: {
          data: fields
        },
        xAxis: {
          type: 'time',

          splitLine: {
            show: true
          }
        },
        yAxis: [
          {
            name: 'view',
            type: 'value'
          },
          {
            name: 'other',
            type: 'value'
          }
        ],
        series: fields.map(field => {
          return {
            name: field,
            type: 'line',
            showSymbol: false,
            hoverAnimation: false,
            yAxisIndex: field == 'view' ? 0 : 1,
            data: videoStatList.filter(v => v[field] > 0).map(v => {
              return {
                name: field,
                value: [v.ctime, v[field]]
              }
            })
          }
        })
      }
      return obj
    },
    sliderChanged(left, right) {
      // console.log(left,right)
      const timeranger = [ +moment(left), +moment(right) ]
      this.fetchVideoStat(timeranger)
    },
    str2timestamp(str){
      return +moment(str)
    },
    timestamp2str(arr){
      return arr.map(v => moment(v).format('YYYY-MM-DD HH:mm:ss'))
    }
  }
}
</script>
