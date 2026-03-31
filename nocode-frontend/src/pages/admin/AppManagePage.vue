<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { listAppVOsByPageByAdmin, deleteAppByAdmin, updateAppByAdmin } from '@/api/appController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import AppEditPage from '@/pages/app/AppEditPage.vue'

const editModalRef = ref<InstanceType<typeof AppEditPage>>()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
  },
  {
    title: '应用封面',
    dataIndex: 'cover',
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
  },
  {
    title: '优先级',
    dataIndex: 'priority',
  },
  {
    title: '用户',
    dataIndex: 'user',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)
const displayModal = ref(false)
const currentAppId = ref('')

const searchParams = reactive<API.AppAdminQueryRequest>({
  pageNum: 1,
  pageSize: 20,
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVOsByPageByAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.msg)
    }
  } catch (error: any) {
    message.error('获取数据失败：' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pagination: any) => {
  searchParams.pageNum = pagination.current
  searchParams.pageSize = pagination.pageSize
  fetchData()
}

const handleDelete = async (id: string) => {
  try {
    const res = await deleteAppByAdmin({ id: id })
    if (res.data.code === 0 && res.data.data) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败：' + (res.data.msg || '未知错误'))
    }
  } catch (error: any) {
    message.error('删除失败：' + (error.message || '网络错误'))
  }
}

const handleFeature = async (id: string, priority: number) => {
  try {
    if (priority !== 99) {
      const res = await updateAppByAdmin({
        id: id,
        priority: 99,
      })
      if (res.data.code === 0 && res.data.data) {
        message.success('设置精选成功')
        fetchData()
      } else {
        message.error('设置精选失败：' + (res.data.msg || '未知错误'))
      }
    } else {
      const res = await updateAppByAdmin({
        id: id,
        priority: 0,
      })
      if (res.data.code === 0 && res.data.data) {
        message.success('取消精选成功')
        fetchData()
      } else {
        message.error('取消精选失败：' + (res.data.msg || '未知错误'))
      }
    }
  } catch (error: any) {
    message.error('设置精选失败：' + (error.message || '网络错误'))
  }
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const handleOk = async () => {
  if (editModalRef.value) {
    await editModalRef.value.handleSave()
  }
  await fetchData()
  displayModal.value = false
  currentAppId.value = ''
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="appManagePage">
    <a-card title="应用管理">
      <!-- 搜索表单 -->
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="应用名称">
          <a-input v-model:value="searchParams.appName" placeholder="输入应用名称" />
        </a-form-item>
        <a-form-item label="生成类型">
          <a-input v-model:value="searchParams.codeGenType" placeholder="输入生成类型" />
        </a-form-item>
        <a-form-item label="用户ID">
          <a-input v-model:value="searchParams.userId" placeholder="输入用户ID" type="number" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>
      <a-divider />
      <!-- 表格 -->

      <a-divider />

      <a-table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="{
          current: searchParams.pageNum,
          pageSize: searchParams.pageSize,
          total: total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total: number) => `共 ${total} 条`,
        }"
        :scroll="{ x: 1200 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'cover'">
            <a-avatar v-if="record.cover" :src="record.cover" :size="40" />
            <a-avatar v-else :size="40">{{ record.appName?.charAt(0) || 'A' }}</a-avatar>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-else-if="column.dataIndex === 'user'">
            {{ record.user?.userName || '未知' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button
                size="small"
                type="primary"
                @click="displayModal = true;currentAppId = record.id;console.log(currentAppId)"
              >
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除该应用吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
              <a-button
                size="small"
                :type="record.priority === 99 ? 'default' : 'dashed'"
                @click="handleFeature(record.id, record.priority)"
              >
                {{ record.priority === 99 ? '已精选' : '精选' }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- 编辑应用模态框 -->
      <a-modal
        :open="displayModal"
        title="编辑应用信息"
        @ok="handleOk"
        @cancel="displayModal = false"
      >
        <AppEditPage ref="editModalRef" v-bind:id="currentAppId" />
      </a-modal>
    </a-card>
  </div>
</template>

<style scoped></style>
