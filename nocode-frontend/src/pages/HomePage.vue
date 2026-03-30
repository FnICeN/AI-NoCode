<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  addApp,
  listMyAppVoByPage,
  listFeaturedApps,
} from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户输入的提示词
const prompt = ref('')
const creating = ref(false)

// 应用类型定义
interface AppVO {
  id?: number
  appName?: string
  cover?: string
  codeGenType?: string
  priority?: number
  userId?: number
  createTime?: string
  updateTime?: string
  user?: {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }
}

// 我的应用列表
const myApps = ref<AppVO[]>([])
const myAppsLoading = ref(false)
const myAppsTotal = ref(0)
const myAppsPage = ref(1)
const myAppsPageSize = ref(6)

// 精选应用列表
const featuredApps = ref<AppVO[]>([])
const featuredAppsLoading = ref(false)
const featuredAppsTotal = ref(0)
const featuredAppsPage = ref(1)
const featuredAppsPageSize = ref(6)

// 快捷提示词标签
const quickPrompts = [
  '波普风电商页面',
  '企业网站',
  '电商运营后台',
  '暗黑话题社区',
]

// 创建应用
const handleCreateApp = async () => {
  if (!prompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  creating.value = true
  try {
    const res = await addApp({ initPrompt: prompt.value.trim() })
    if (res.data.code === 0 && res.data.data) {
      const appId = res.data.data
      message.success('应用创建成功，即将进入对话页面')
      router.push(`/app/chat/${appId}?gen=1`)
    } else {
      message.error(res.data.msg || '创建应用失败')
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '网络错误'
    message.error('创建应用失败：' + errorMessage)
  } finally {
    creating.value = false
  }
}

// 使用快捷提示词
const useQuickPrompt = (text: string) => {
  prompt.value = `使用 NoCode 创建一个${text}`
}

// 获取我的应用列表
const fetchMyApps = async () => {
  if (!loginUserStore.loginUser.id) return
  myAppsLoading.value = true
  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.value,
      pageSize: myAppsPageSize.value,
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('获取我的应用失败', error)
  } finally {
    myAppsLoading.value = false
  }
}

// 获取精选应用列表
const fetchFeaturedApps = async () => {
  featuredAppsLoading.value = true
  try {
    const res = await listFeaturedApps({
      pageNum: featuredAppsPage.value,
      pageSize: featuredAppsPageSize.value,
    })
    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('获取精选应用失败', error)
  } finally {
    featuredAppsLoading.value = false
  }
}

// 分页切换
const handleMyAppsPageChange = (page: number) => {
  myAppsPage.value = page
  fetchMyApps()
}

const handleFeaturedAppsPageChange = (page: number) => {
  featuredAppsPage.value = page
  fetchFeaturedApps()
}

// 跳转到应用对话页
const goToAppChat = (appId: number) => {
  router.push(`/app/chat/${appId}?gen=0`)
}

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 0 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days < 7) {
    return `${days}天前`
  } else if (days < 30) {
    return `${Math.floor(days / 7)}周前`
  } else {
    return `${Math.floor(days / 30)}月前`
  }
}

onMounted(() => {
  fetchMyApps()
  fetchFeaturedApps()
})
</script>

<template>
  <div class="home-page">
    <!-- 网站标题区域 -->
    <div class="hero-section">
      <h1 class="main-title">
        一句话
        <span class="cat-icon">🐱</span>
        呈所想
      </h1>
      <p class="sub-title">与 AI 对话轻松创建应用和网站</p>

      <!-- 输入框区域 -->
      <div class="input-section">
        <a-textarea
          v-model:value="prompt"
          class="prompt-input"
          placeholder="使用 NoCode 创建一个高效的小工具，帮我计算......"
          :rows="4"
          @pressEnter.prevent="handleCreateApp"
        />
        <div class="input-actions">
          <div class="left-actions">
            <a-button type="text" class="action-btn">
              <template #icon><paper-clip-outlined /></template>
              上传
            </a-button>
            <a-button type="text" class="action-btn">
              <template #icon><thunderbolt-outlined /></template>
              优化
            </a-button>
          </div>
          <a-button
            type="primary"
            shape="circle"
            size="large"
            class="send-btn"
            :loading="creating"
            @click="handleCreateApp"
          >
            <arrow-up-outlined />
          </a-button>
        </div>
      </div>

      <!-- 快捷提示词 -->
      <div class="quick-prompts">
        <a-tag
          v-for="(text, index) in quickPrompts"
          :key="index"
          class="quick-tag"
          @click="useQuickPrompt(text)"
        >
          {{ text }}
        </a-tag>
      </div>
    </div>

    <!-- 我的作品区域 -->
    <div v-if="loginUserStore.loginUser.id" class="section">
      <h2 class="section-title">我的作品</h2>
      <a-spin :spinning="myAppsLoading">
        <a-empty v-if="myApps.length === 0 && !myAppsLoading" description="暂无应用，快去创建一个吧" />
        <a-row v-else :gutter="[24, 24]">
          <a-col
            v-for="app in myApps"
            :key="app.id"
            :xs="24"
            :sm="12"
            :md="8"
          >
            <a-card
              class="app-card"
              hoverable
              @click="goToAppChat(app.id!)"
            >
              <template #cover>
                <div class="app-cover">
                  <img
                    v-if="app.cover"
                    :src="app.cover"
                    :alt="app.appName"
                  />
                  <div v-else class="cover-placeholder">
                    <appstore-outlined />
                  </div>
                </div>
              </template>
              <a-card-meta>
                <template #title>
                  <span class="app-name">{{ app.appName || '未命名应用' }}</span>
                </template>
                <template #description>
                  <span class="app-time">创建于 {{ formatTime(app.createTime) }}</span>
                </template>
              </a-card-meta>
            </a-card>
          </a-col>
        </a-row>
        <div v-if="myAppsTotal > myAppsPageSize" class="pagination-wrapper">
          <a-pagination
            v-model:current="myAppsPage"
            :total="myAppsTotal"
            :pageSize="myAppsPageSize"
            @change="handleMyAppsPageChange"
          />
        </div>
      </a-spin>
    </div>

    <!-- 精选案例区域 -->
    <div class="section">
      <h2 class="section-title">精选案例</h2>
      <a-spin :spinning="featuredAppsLoading">
        <a-empty v-if="featuredApps.length === 0 && !featuredAppsLoading" description="暂无精选应用" />
        <a-row v-else :gutter="[24, 24]">
          <a-col
            v-for="app in featuredApps"
            :key="app.id"
            :xs="24"
            :sm="12"
            :md="8"
          >
            <a-card
              class="app-card featured-card"
              hoverable
              @click="goToAppChat(app.id!)"
            >
              <template #cover>
                <div class="app-cover">
                  <img
                    v-if="app.cover"
                    :src="app.cover"
                    :alt="app.appName"
                  />
                  <div v-else class="cover-placeholder">
                    <appstore-outlined />
                  </div>
                </div>
              </template>
              <a-card-meta>
                <template #title>
                  <div class="featured-title">
                    <a-avatar
                      v-if="app.user?.userAvatar"
                      :src="app.user.userAvatar"
                      size="small"
                    />
                    <a-avatar v-else size="small">
                      {{ app.user?.userName?.charAt(0) || 'U' }}
                    </a-avatar>
                    <span class="app-name">{{ app.appName || '未命名应用' }}</span>
                    <a-tag v-if="app.codeGenType" size="small" color="blue">
                      {{ app.codeGenType }}
                    </a-tag>
                  </div>
                </template>
                <template #description>
                  <span class="app-author">{{ app.user?.userName || '匿名用户' }}</span>
                </template>
              </a-card-meta>
            </a-card>
          </a-col>
        </a-row>
        <div v-if="featuredAppsTotal > featuredAppsPageSize" class="pagination-wrapper">
          <a-pagination
            v-model:current="featuredAppsPage"
            :total="featuredAppsTotal"
            :pageSize="featuredAppsPageSize"
            @change="handleFeaturedAppsPageChange"
          />
        </div>
      </a-spin>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  padding: 40px 0;
}

.hero-section {
  text-align: center;
  margin-bottom: 60px;
}

.main-title {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 16px;
  color: #1f1f1f;
}

.cat-icon {
  display: inline-block;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  line-height: 48px;
  font-size: 24px;
  margin: 0 8px;
  vertical-align: middle;
}

.sub-title {
  font-size: 18px;
  color: #666;
  margin-bottom: 32px;
}

.input-section {
  max-width: 720px;
  margin: 0 auto 16px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 16px;
}

.prompt-input {
  border: none;
  resize: none;
  font-size: 16px;
  background: transparent;
}

.prompt-input:focus {
  box-shadow: none;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.left-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  color: #666;
}

.send-btn {
  background: #1f1f1f;
  border-color: #1f1f1f;
}

.send-btn:hover {
  background: #333;
  border-color: #333;
}

.quick-prompts {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.quick-tag {
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  border-radius: 20px;
  background: #f5f5f5;
  border: none;
  color: #666;
}

.quick-tag:hover {
  background: #e8e8e8;
  color: #333;
}

.section {
  margin-bottom: 48px;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #1f1f1f;
}

.app-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.app-cover {
  height: 160px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  font-size: 48px;
  color: #ccc;
}

.app-name {
  font-size: 16px;
  font-weight: 500;
  color: #1f1f1f;
}

.app-time {
  font-size: 13px;
  color: #999;
}

.featured-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-author {
  font-size: 13px;
  color: #999;
}

.pagination-wrapper {
  margin-top: 24px;
  text-align: center;
}

.featured-card :deep(.ant-card-body) {
  padding: 12px;
}
</style>
