<style scoped>
.layout {
  border: 1px solid #d7dde4;
  background: #f5f7f9;
  position: relative;
  border-radius: 4px;
  /* overflow: hidden; */
}
.layout-header-bar {
  background: #fff;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.1);
}
</style>
<template>
    <div class="layout">
        <Sider :style="{position: 'fixed', height: '100vh', left: 0, overflow: 'auto', zIndex:'99'}">
            <Menu :active-name="this.$route.params.subpage" theme="dark" width="auto" :open-names="['tracer']"  @on-select="onMenuSelect">
                <Submenu name="tracer">
                    <template slot="title">
                        <Icon type="ios-navigate"></Icon>
                        Tracer
                    </template>
                    <MenuItem name="member">Member</MenuItem>
                    <MenuItem name="video">Video</MenuItem>
                    <MenuItem name="faq">FAQ</MenuItem>
                    <MenuItem name="little-nest">Little Nest (Beta)</MenuItem>
                </Submenu>
            </Menu>
        </Sider>
        <Layout :style="{marginLeft: '200px',  minWidth: '1200px'}">
            <Header :style="{background: '#fff', boxShadow: '0 2px 3px 2px rgba(0,0,0,.1)'}">
                <div :style="{float:'right', marginTop:'20px',lineHeight:'20px',fontSize:'16px'}">
                    <Dropdown @on-click="onLogout" placement="bottom-start">
                        <a href="javascript:void(0)">
                            {{showUsername()}}
                            <Icon type="arrow-down-b"></Icon>
                        </a>
                        <DropdownMenu slot="list">
                            <DropdownItem>logout</DropdownItem>
                        </DropdownMenu>
                    </Dropdown>
                </div>
                <h1 @click="gotoHome" :style="{cursor:'pointer'}">Video Tracer of Bilibili</h1>
            </Header>
            <Content :style="{padding: '16px'}">
                <!-- <Breadcrumb :style="{margin: '16px 0'}">
                    <BreadcrumbItem>Tracer</BreadcrumbItem>
                    <BreadcrumbItem>{{this.$route.params.subpage}}</BreadcrumbItem>
                </Breadcrumb> -->
                <Card>
                    <div style="min-height: 75vh">
                      <PageSwitch :subpage="this.$route.params.subpage"></PageSwitch>
                    </div>
                </Card>
            </Content>
        </Layout>
    </div>
</template>
<script>
import PageSwitch from '../components/PageSwitch.vue'
const GUEST_NAME = '# Guest'
export default {
  components: { PageSwitch },
  methods: {
    gotoHome(){
        this.$router.push('/')
    },
    showUsername(){
        const username = sessionStorage.getItem('username')
        if(!username){
            this.$api.get('/userInfo',{},r=>{
                sessionStorage.setItem('username', r.data.user || GUEST_NAME)
            })
        }
        return username || GUEST_NAME
    },
    onMenuSelect(name) {
      console.log('go to', name)
      this.$router.push('/tracer/' + name)
    },
    onLogout(name){
        this.$api.get('/logout',{},r=>{
            console.log('logout')
            sessionStorage.removeItem('username')
            this.$router.push('/')
        })
    }
  }
}
</script>