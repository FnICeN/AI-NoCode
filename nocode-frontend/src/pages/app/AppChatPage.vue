<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getAppById, deployApp } from '@/api/appController'
import { listChatHistoryByAppId } from '@/api/chatHistoryController'
import {
  ArrowLeftOutlined,
  InfoCircleOutlined,
  CloudUploadOutlined,
  UserOutlined,
  RobotOutlined,
  PaperClipOutlined,
  EditOutlined,
  ThunderboltOutlined,
  DesktopOutlined,
  ExportOutlined,
  ArrowUpOutlined,
} from '@ant-design/icons-vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'

const route = useRoute()
const router = useRouter()
const appId = route.params.appId as string

// 应用信息
const app = ref<API.App>()
const appLoading = ref(false)

// 消息列表
interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  isStreaming?: boolean
}

const messages = ref<Message[]>([])
const displayModal = ref(false)
const inputMessage = ref('')
const sending = ref(false)
const generating = ref(false)

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(true)
const lastCreateTime = ref<string>('')

// 网页预览
const previewUrl = ref('')
const showPreview = ref(false)

// 消息容器引用
const messageContainerRef = ref<HTMLDivElement>()

// 部署相关
const deploying = ref(false)
const deployUrl = ref('')
const deployModalVisible = ref(false)

// AppEditPage组件引用
const appEditPageRef = ref<InstanceType<typeof AppEditPage>>()

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId || loadingHistory.value || (!isLoadMore && messages.value.length > 0)) return

  loadingHistory.value = true
  try {
    const res = await listChatHistoryByAppId({
      appId: appId,
      lastCreateTime: isLoadMore ? lastCreateTime.value : undefined,
    })

    if (res.data.code === 0 && res.data.data) {
      const chatHistory = res.data.data.records || []

      if (chatHistory.length < 10) {
        hasMoreHistory.value = false
      }

      if (chatHistory.length > 0) {
        lastCreateTime.value = chatHistory[chatHistory.length - 1].createTime || ''

        const newMessages = chatHistory.map((item) => ({
          id: String(item.id),
          role: item.messageType === 'user' ? 'user' : 'assistant',
          content: item.message || '',
        }))

        if (isLoadMore) {
          messages.value = [...newMessages, ...messages.value]
        } else {
          messages.value = newMessages.reverse()
        }
      }

      // 如果是首次加载且没有对话历史，并且是自己的app，自动发送初始消息
      if (!isLoadMore && chatHistory.length === 0) {
        const userMessage : Message = {
          id: appId,
          role: 'user',
          content: app.value?.initPrompt,
        }
        messages.value.push(userMessage)
        await sendInitialMessage(String(app.value?.initPrompt || ''))
      }
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '网络错误'
    message.error('加载对话历史失败：' + errorMessage)
  } finally {
    loadingHistory.value = false
  }
}

// 获取应用详情
const fetchAppDetail = async () => {
  if (!appId) {
    message.error('应用ID无效')
    router.push('/')
    return
  }
  appLoading.value = true
  try {
    const res = await getAppById({ id: appId })
    if (res.data.code === 0 && res.data.data) {
      app.value = res.data.data
      // 展示预览
      if (res.data.data.id && res.data.data.codeGenType) {
        previewUrl.value = `http://localhost:8080/api/static/${res.data.data.codeGenType}_${res.data.data.id}/`
        showPreview.value = true
      }
      // 加载对话历史
      await loadChatHistory()
    } else {
      message.error(res.data.msg || '获取应用信息失败')
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '网络错误'
    message.error('获取应用信息失败：' + errorMessage)
  } finally {
    appLoading.value = false
  }
}

// 构建页面信息 (不再使用，已整合到loadChatHistory中)
const constructPageInfo = (data: API.App) => {
  // 展示预览
  if (data.id && data.codeGenType) {
    previewUrl.value = `http://localhost:8080/api/static/${data.codeGenType}_${data.id}/`
    showPreview.value = true
  }
}

// 发送初始消息
const sendInitialMessage = async (initPrompt: string) => {
  // 开始AI对话
  await startChat(initPrompt)
}

// 开始AI对话（SSE流式）
const startChat = async (messageText: string) => {
  generating.value = true
  showPreview.value = false
  previewUrl.value = ''

  // 添加AI消息占位
  const aiMessage = ref<Message>({
    id: (Date.now() + 1).toString(),
    role: 'assistant',
    content: '',
    isStreaming: true,
  })
  messages.value.push(aiMessage.value)

  try {
    const url = `http://localhost:8080/api/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(messageText)}`
    const eventSource = new EventSource(url, { withCredentials: true })

    let fullContent = ''
    let streamCompleted = false

    // 连接建立
    eventSource.onopen = () => {
      console.log('SSE连接已建立')
    }

    // 接收消息
    eventSource.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data)
        // 后端返回格式是 {"d":"内容"}
        if (parsed.d) {
          fullContent += parsed.d
          aiMessage.value.content = fullContent
          scrollToBottom()
        }
      } catch (error) {
        // 如果不是JSON，直接追加
        fullContent += event.data
        aiMessage.value.content = fullContent
        scrollToBottom()
      }
    }

    // 监听done事件
    eventSource.addEventListener('done', () => {
      streamCompleted = true
      aiMessage.value.isStreaming = false

      // 延迟1秒更新预览界面，确保后端完成处理
      setTimeout(() => {
        if (app.value?.codeGenType) {
          previewUrl.value = `http://localhost:8080/api/static/${app.value.codeGenType}_${appId}/`
          showPreview.value = true
        }
      }, 1000)

      // 关闭连接
      eventSource.close()
      generating.value = false
      scrollToBottom()
    })

    // 错误处理
    eventSource.onerror = (error) => {
      // 检查是否是正常的连接关闭
      if (streamCompleted) {
        // 正常关闭，不处理
        return
      }

      const errorMessage = error instanceof Event ? '连接错误' : '网络错误'
      message.error('对话失败：' + errorMessage)
      aiMessage.value.content = '抱歉，发生了错误，请重试。'
      aiMessage.value.isStreaming = false

      // 关闭连接
      eventSource.close()
      generating.value = false
      scrollToBottom()
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '网络错误'
    message.error('对话失败：' + errorMessage)
    aiMessage.value.content = '抱歉，发生了错误，请重试。'
    aiMessage.value.isStreaming = false
    generating.value = false
    scrollToBottom()
  }
}

// 发送消息
const handleSendMessage = async () => {
  if (!inputMessage.value.trim() || sending.value || generating.value) return

  const messageText = inputMessage.value.trim()
  inputMessage.value = ''

  // 添加用户消息
  const userMessage: Message = {
    id: Date.now().toString(),
    role: 'user',
    content: messageText,
  }
  messages.value.push(userMessage)

  scrollToBottom()

  // 开始AI对话
  await startChat(messageText)
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainerRef.value) {
      messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight
    }
  })
}

// 部署应用
const handleDeploy = async () => {
  if (!appId) return
  deploying.value = true
  try {
    const res = await deployApp({ appId })
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error(res.data.msg || '部署失败')
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '网络错误'
    message.error('部署失败：' + errorMessage)
  } finally {
    deploying.value = false
  }
}

const handleEdit = (id: string) => {
  displayModal.value = true
}

// 复制部署链接
const copyDeployUrl = () => {
  navigator.clipboard.writeText(deployUrl.value)
  message.success('链接已复制')
}

// 打开部署链接
const openDeployUrl = () => {
  window.open(deployUrl.value, '_blank')
}

// 处理模态框确定按钮
const handleModalOk = async () => {
  // 调用AppEditPage组件的保存方法
  if (appEditPageRef.value) {
    await appEditPageRef.value.handleSave()
  }
  // 保存成功后，刷新应用信息
  await fetchAppDetail()
  // 关闭模态框
  displayModal.value = false
}

// 格式化消息内容（支持代码块）
const formatMessage = (content: string) => {
  // 简单处理，将代码块用pre标签包裹
  return content.replace(/```([\s\S]*?)```/g, '<pre class="code-block">$1</pre>')
}

onMounted(() => {
  fetchAppDetail()
})

// 监听消息变化，自动滚动
watch(messages, scrollToBottom, { deep: true })
</script>

<template>
  <div class="chat-page">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <div class="header-left">
        <a-button type="text" @click="router.push('/')">
          <template #icon><arrow-left-outlined /></template>
        </a-button>
        <span class="app-name">{{ app?.appName || '未命名应用' }}</span>
        <a-tag v-if="app?.codeGenType" size="small" color="blue">
          {{ app.codeGenType }}
        </a-tag>
      </div>
      <div class="header-right">
        <a-button @click="handleEdit(app?.id || '')">
          <template #icon><info-circle-outlined /></template>
          应用详情
        </a-button>
        <a-modal
          title="应用信息修改"
          :open="displayModal"
          @cancel="displayModal = false"
          @ok="handleModalOk"
        >
          <AppEditPage ref="appEditPageRef" v-bind:id="appId" />
        </a-modal>
        <a-button type="primary" :loading="deploying" @click="handleDeploy">
          <template #icon><cloud-upload-outlined /></template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div class="chat-content">
      <!-- 左侧对话区域 -->
      <div class="chat-left">
        <!-- 消息区域 -->
        <div ref="messageContainerRef" class="message-container">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory && messages.length > 0" class="load-more">
            <a-button type="link" :loading="loadingHistory" @click="loadChatHistory(true)">
              加载更多历史消息
            </a-button>
          </div>

          <div v-if="messages.length === 0 && !appLoading" class="empty-messages">
            <a-empty description="开始和 AI 对话生成应用吧" />
          </div>
          <div v-else class="messages-list">
            <div v-for="msg in messages" :key="msg.id" :class="['message-item', msg.role]">
              <div class="message-avatar">
                <a-avatar v-if="msg.role === 'assistant'" style="background-color: #1890ff">
                  <template #icon><robot-outlined /></template>
                </a-avatar>
                <a-avatar v-else style="background-color: #52c41a">
                  <template #icon><user-outlined /></template>
                </a-avatar>
              </div>
              <div class="message-bubble">
                <div class="message-text" v-html="formatMessage(msg.content)" />
              </div>
            </div>
          </div>
        </div>

        <!-- 输入框区域 -->
        <div class="input-area">
          <div class="input-wrapper">
            <a-textarea
              v-model:value="inputMessage"
              class="message-input"
              placeholder="描述越详细，页面越具体，可以一步一步完善生成效果..."
              :rows="3"
              :disabled="generating"
              @pressEnter.prevent="handleSendMessage"
            />
            <div class="input-actions">
              <div class="left-actions">
                <a-button type="text" size="small">
                  <template #icon><paper-clip-outlined /></template>
                  上传
                </a-button>
                <a-button type="text" size="small">
                  <template #icon><edit-outlined /></template>
                  编辑
                </a-button>
                <a-button type="text" size="small">
                  <template #icon><thunderbolt-outlined /></template>
                  优化
                </a-button>
              </div>
              <a-button
                type="primary"
                shape="circle"
                :loading="sending || generating"
                :disabled="!inputMessage.trim() || generating"
                @click="handleSendMessage"
              >
                <arrow-up-outlined />
              </a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧预览区域 -->
      <div class="chat-right">
        <div v-if="!showPreview" class="preview-placeholder">
          <a-empty description="生成完成后将在此展示网页效果">
            <template #image>
              <desktop-outlined style="font-size: 64px; color: #ccc" />
            </template>
          </a-empty>
        </div>
        <div v-else class="preview-container">
          <div class="preview-header">
            <span class="preview-title">生成后的网页展示</span>
            <a-button type="link" @click="showPreview = false"> 隐藏 </a-button>
          </div>
          <iframe
            :src="previewUrl"
            class="preview-frame"
            sandbox="allow-scripts allow-same-origin allow-forms"
          />
        </div>
      </div>
    </div>

    <!-- 部署成功弹窗 -->
    <a-modal
      v-model:open="deployModalVisible"
      title="部署成功"
      :footer="null"
      @cancel="deployModalVisible = false"
    >
      <div class="deploy-result">
        <p>您的应用已成功部署，可以通过以下链接访问：</p>
        <a-input-group compact>
          <a-input v-model:value="deployUrl" readonly style="width: calc(100% - 80px)" />
          <a-button type="primary" @click="copyDeployUrl">复制</a-button>
        </a-input-group>
        <a-button type="link" style="margin-top: 16px" @click="openDeployUrl">
          在新窗口打开 <export-outlined />
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.chat-page {
  height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.chat-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-name {
  font-size: 16px;
  font-weight: 500;
  color: #1f1f1f;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.chat-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 400px;
  max-width: 600px;
  border-right: 1px solid #e8e8e8;
  background: #fff;
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.empty-messages {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.load-more {
  text-align: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #f5f5f5;
}

.message-item.user .message-bubble {
  background: #1890ff;
  color: #fff;
}

.message-text {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-text :deep(.code-block) {
  background: #1f1f1f;
  color: #fff;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.streaming-indicator {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

.streaming-indicator .dot {
  width: 6px;
  height: 6px;
  background: #999;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.streaming-indicator .dot:nth-child(1) {
  animation-delay: -0.32s;
}

.streaming-indicator .dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.input-area {
  padding: 16px 24px;
  border-top: 1px solid #e8e8e8;
  background: #fff;
}

.input-wrapper {
  background: #f5f5f5;
  border-radius: 12px;
  padding: 12px;
}

.message-input {
  border: none;
  resize: none;
  background: transparent;
}

.message-input:focus {
  box-shadow: none;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.left-actions {
  display: flex;
  gap: 8px;
}

.chat-right {
  flex: 1;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.preview-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.preview-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #e8e8e8;
}

.preview-title {
  font-size: 14px;
  color: #666;
}

.preview-frame {
  flex: 1;
  width: 100%;
  border: none;
}

.deploy-result {
  padding: 16px 0;
}
</style>
