<template>
<div>
  <transition name="slide-fade">
    <div v-if="mainTab" key="traceMember">
        <Row>
          <Col span="20"> 
            <Input v-model="name" placeholder="用户名称/mid" style="width: 300px">
                <Select v-model="filterSelectValue" slot="prepend" style="width: 80px" @on-change="filterSearch">
                    <Option value="all">All</Option>
                    <Option value="keep">Keep on</Option>
                </Select>
                <Button slot="append" icon="ios-search"  @click="filterSearch"></Button>
            </Input>
          </Col>
          <Col span="4"><Button type="primary" @click="renderModal">Trace new member</Button></Col>
        </Row>
        <br/>
        <Table stripe :loading="loading" :columns="columns" :data="data"></Table>
        <br/>
        <Page :total="total" :page-size="pageSize" :current="page" show-total class="paging" @on-change="fetchData"></Page>
    </div>
    <div v-else key="memberInfo">
        <Button type="primary" @click="mainTab=true">Back</Button>
        <br/> <br/>
        <MemberInfo :record="record"></MemberInfo>
    </div>
  </transition>
</div>
</template>
<script>
import MemberInfo from './MemberInfo.vue'
import moment from 'moment'
export default {
  components: {
    MemberInfo
  },
  data() {
    return {
      mainTab: true,
      record: {},
      loading: true,
      columns: [
        {
          title: 'Name',
          key: 'name',
          render: (h, data) => {
            return h('div', [
              h('img', {
                attrs: { src: data.row.face, height: '40px' },
                style: { verticalAlign: 'middle', marginRight: '5px', cursor: 'pointer' },
                on: {
                  click: () => window.open(`https://space.bilibili.com/${data.row.mid}#/`, '_blank')
                }
              }),
              h('span', {}, data.row.name)
            ])
          }
        },
        {
          title: 'Mid',
          key: 'mid'
        },
        {
          title: 'Record Time',
          key: 'ctime',
          render: (h, data) => h('span', moment(data.row.ctime).format('YYYY-MM-DD HH:mm:ss'))
        },
        {
          title: 'Keep on',
          key: 'keep',
          render: (h, data) => {
            return h('i-switch', {
              props: {
                type: 'primary',
                value: data.row.keep === 1 //控制开关的打开或关闭状态，官网文档属性是value
              },
              on: {
                //触发事件是on-change,用引号括起来
                'on-change': value =>
                  this.toggleKeepStatus(data.row.mid, value ? 1 : 0, data.row.name)
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
          width: 240,
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
                    click: () => this.memberInfo(params.row)
                  }
                },
                'View'
              ),
              h(
                'Button',
                {
                  props: {
                    type: 'error',
                    size: 'small'
                  },
                  style: {
                    marginRight: '5px'
                  },
                  on: {
                    click: () => this.deleteMember(params.row)
                  }
                },
                'Delete'
              )
            ])
          }
        }
      ],
      data: [],
      page: 1,
      total: 0,
      pageSize: 20,
      name: '',
      filterSelectValue: 'all',
    }
  },
  methods: {
    filterSearch(){
      this.fetchData();
    },
    fetchData(page = 1) {
      this.page = page
      const param = {
        pageNum:page, 
        name: this.name, 
        keep: this.filterSelectValue === 'keep' ? 1 : null
      }
      this.$api.get(`/trace/member`, param, r => {
        this.data = r.data.list
        this.total = r.data.total
        this.pageSize = r.data.pageSize
        this.loading = false
      })
    },
    toggleKeepStatus(mid, keep, name) {
      this.$api.put(`/trace/member/${mid}/keep/${keep}`, {}, r => {
        if (r.data.effect > 0) {
          //
        } else {
          this.$Notice.error({
            title: `Oops! ${name} is out of control.`
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
              placeholder: 'Please enter mid'
            },
            on: {
              input: val => (tempValue = val)
            }
          })
        },
        onOk() {
          vee.addMember(tempValue)
        }
      })
    },
    addMember(mid) {
      if (Number.isInteger(Number(mid)) == false) {
        this.$Notice.error({
          title: `Oops! "${mid}" is invalid mid`
        })
        return
      }
      this.$api.post(`/trace/member/${mid}`, {}, r => {
        if (r.data.effect > 0) {
          this.fetchData()
          this.$Notice.success({
            title: `Great! we are tracing ${mid}`
          })
        } else {
          this.$Notice.error({
            title: `Oops! ${mid} is dups or invalid`
          })
        }
      })
    },
    deleteMember({ mid, name }) {
      this.$Modal.confirm({
        title: 'Delete Member',
        content: `<h4>Really to delete ${name}[${mid}] ?</h4>
                  <p>*删除后追踪数据不再保留</p>`,
        onOk: () => {
          this.$api.delete(`/trace/member/${mid}`, {}, r => {
            if (r.data.effect > 0) {
              this.fetchData()
              this.$Notice.success({
                title: `Yoo! ${name} gone.`
              })
            } else {
              this.$Notice.error({
                title: `Oops! ${name} is still here.`
              })
            }
          })
        },
        onCancel: () => {
          // this.$Message.info('nothing happen.')
        }
      })
    },
    memberInfo(record) {
      this.record = record
      this.mainTab = false
    }
  },
  created() {
    this.fetchData()
  }
}
</script>
