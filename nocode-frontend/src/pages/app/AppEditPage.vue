<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { getAppById, updateApp, updateAppByAdmin } from '@/api/appController.ts'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const loginUserStore = useLoginUserStore()

const app = ref<API.App>()
const loading = ref(false)
const saving = ref(false)

const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
})
const props = defineProps({
  id: String,
})

// 响应式appId
const appId = ref(props.id || (route.params.appId as string))

// 监听id prop变化
watch(
  () => props.id,
  (newId) => {
    if (newId) {
      appId.value = newId
      fetchAppDetail()
    }
  },
)

const fetchAppDetail = async () => {
  if (!appId.value) {
    message.error('应用ID无效')
    return
  }
  loading.value = true
  try {
    const res = await getAppById({ id: appId.value })
    if (res.data.code === 0 && res.data.data) {
      app.value = res.data.data
      formData.appName = res.data.data.appName || ''
      formData.cover = res.data.data.cover || ''
      formData.priority = res.data.data.priority || 0

      // 检查权限：普通用户只能编辑自己的应用
      const loginUser = loginUserStore.loginUser
      if (
        loginUser &&
        loginUser.userRole !== 'admin' &&
        String(loginUser.id) !== String(res.data.data.userId)
      ) {
        message.error('没有权限编辑此应用')
      }
    } else {
      message.error(res.data.msg || '获取应用信息失败')
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '网络错误'
    message.error('获取应用信息失败：' + errorMessage)
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!app.value) return

  saving.value = true
  try {
    const loginUser = loginUserStore.loginUser
    let res

    if (loginUser && loginUser.userRole === 'admin') {
      // 管理员可以修改更多字段
      res = await updateAppByAdmin({
        id: appId.value,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority,
      })
    } else {
      // 普通用户只能修改应用名称
      res = await updateApp({
        id: appId.value,
        appName: formData.appName,
      })
    }

    if (res.data.code === 0 && res.data.data) {
      message.success('保存成功')
    } else {
      message.error(res.data.msg || '保存失败')
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '网络错误'
    message.error('保存失败：' + errorMessage)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  // 确保获取登录用户信息
  if (!loginUserStore.loginUser) {
    await loginUserStore.fetchLoginUser()
  }
  await fetchAppDetail()
})

defineExpose({
  handleSave,
})
</script>

<template>
  <div class="appEditPage">
    <a-card title="应用信息修改">
      <a-form v-if="app" layout="vertical" :model="formData" :loading="loading">
        <a-form-item label="应用名称" required>
          <a-input v-model:value="formData.appName" placeholder="输入应用名称" />
        </a-form-item>

        <a-form-item label="应用封面" v-if="loginUserStore.loginUser?.userRole === 'admin'">
          <a-input v-model:value="formData.cover" placeholder="输入应用封面URL" />
        </a-form-item>

        <a-form-item v-if="loginUserStore.loginUser?.userRole === 'admin'">
          <template #label>
            优先级
            <a-tooltip title="优先级为99的应用会显示在精选列表中">
              <QuestionCircleOutlined style="margin-left: 8px" />
            </a-tooltip>
          </template>
          <a-input-number
            v-model:value="formData.priority"
            min="0"
            max="99"
            placeholder="输入优先级"
          />
        </a-form-item>

        <a-form-item label="生成类型">
          <a-input :value="app.codeGenType" disabled />
        </a-form-item>

        <a-form-item label="创建时间">
          <a-input :value="app.createTime" disabled />
        </a-form-item>
      </a-form>

      <a-spin v-else :spinning="loading">
        <div style="height: 300px; display: flex; align-items: center; justify-content: center">
          加载中...
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped>
.appEditPage {
  max-width: 600px;
  margin: 0 auto;
}
</style>
