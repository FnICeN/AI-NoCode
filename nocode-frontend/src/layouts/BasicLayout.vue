<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />

    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>

    <!-- 底部版权信息 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  background: none;
  min-height: 100vh;
}

.main-content {
  background-image: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.main-content.full-width {
  max-width: 100%;
  padding: 0;
  margin: 0;
  background: transparent;
}
</style>
