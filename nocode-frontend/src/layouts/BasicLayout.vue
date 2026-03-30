<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />

    <!-- 主要内容区域 -->
    <a-layout-content :class="['main-content', { 'full-width': isFullWidthRoute }]">
      <router-view />
    </a-layout-content>

    <!-- 底部版权信息 -->
    <GlobalFooter v-if="!isFullWidthRoute" />
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()

// 判断是否为需要全宽显示的路由（如对话页面）
const isFullWidthRoute = computed(() => {
  return route.path.startsWith('/app/chat')
})
</script>

<style scoped>
.basic-layout {
  background: none;
  min-height: 100vh;
}

.main-content {
  max-width: 1200px;
  padding: 24px;
  background: white;
  margin: 16px auto 56px;
}

.main-content.full-width {
  max-width: 100%;
  padding: 0;
  margin: 0;
  background: transparent;
}
</style>
