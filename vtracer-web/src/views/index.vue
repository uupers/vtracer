<style scoped lang="less">
.index {
  width: 100%;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  text-align: center;
  h1 {
    height: 150px;
    img {
      height: 100%;
    }
  }
  h2 {
    color: #666;
    margin-bottom: 200px;
    p {
      margin: 0 0 50px;
    }
  }
  .ivu-row-flex {
    height: 100%;
  }
}
</style>
<template>
    <div class="index">
        <Row type="flex" justify="center" align="middle">
            <Col span="24">
                <h1>
                    <img src="../images/biliSpider-logo.jpg">
                </h1>
                <h2>
                    <br>
                    <p>Welcome to V-Tracer</p>
                    <div :style="{width:'30%', margin:'auto'}">
                      <Form ref="loginForm" :model="form" :rules="rules">
                          <FormItem prop="username">
                              <Input v-model="form.username" placeholder="请输入用户名">
                                  <span slot="prepend">
                                      <Icon :size="16" type="ios-person-outline"></Icon>
                                  </span>
                              </Input>
                          </FormItem>
                          <FormItem prop="password">
                              <Input type="password" v-model="form.password" placeholder="请输入密码" @on-keypress="onKeyPress">
                                  <span slot="prepend">
                                      <Icon :size="14" type="ios-lock-outline"></Icon>
                                  </span>
                              </Input>
                          </FormItem>
                          <FormItem>
                              <Button @click="handleLogin" type="primary" long>登录</Button>
                              <div :style="{marginTop:'10px'}"></div>
                              <Button @click="handleVisit" type="success" long>随便看看</Button>
                          </FormItem>
                      </Form>
                  </div>
                </h2>
            </Col>
        </Row>
    </div>
</template>
<script>
export default {
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      rules: {
        userName: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
        password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
      }
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.$api.get(`login`, this.form, r => {
            sessionStorage.setItem('username', this.form.username)
            this.handleVisit()
          })
        }
      })
    },
    handleVisit() {
      this.$router.push('/tracer/member')
    },
    onKeyPress(event){
      if(event.key === 'Enter'){
        this.handleLogin()
      }
    }
  }
}
</script>
