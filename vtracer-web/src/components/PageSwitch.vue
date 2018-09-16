<template>
  <div>
    <Member v-if="subpage == 'member'"></Member>
    <Video v-if="subpage == 'video'"></Video>
    <Faq v-if="subpage == 'faq'"></Faq>
    <LittleNest v-if="subpage == 'little-nest'"></LittleNest>
  </div>
</template>

<script>
import Member from '../components/Member.vue';
import Video from '../components/Video.vue';
import Faq from '../components/Faq.vue';
import LittleNest from '../components/LittleNest.vue';
// import VideoStat from '../components/VideoStat.vue';
export default {
  components: { Member, Video ,Faq, LittleNest},
  props:{
    subpage: String
  } 
}
</script>

