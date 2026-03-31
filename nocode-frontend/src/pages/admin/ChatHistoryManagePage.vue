<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { listAllChatHistory, deleteChatHistoryByAppId } from '@/api/chatHistoryController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
  },
  {
    title: '应用ID',
    dataIndex: 'appId',
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
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

const data = ref<API.ChatHistory[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 20,
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAllChatHistory({
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

const handleDelete = async (appId: number) => {
  try {
    const res = await deleteChatHistoryByAppId({ id: appId })
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

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="chatHistoryManagePage">
    <a-card title="对话历史管理">
      <!-- 搜索表单 -->
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="消息类型">
          <a-input v-model:value="searchParams.messageType" placeholder="输入消息类型" />
        </a-form-item>
        <a-form-item label="应用ID">
          <a-input v-model:value="searchParams.appId" placeholder="输入应用ID" type="number" />
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
          <template v-if="column.dataIndex === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-if="column.dataIndex === 'messageType'">
            <a-tag :color="record.messageType === 'user' ? 'green' : 'blue'">
              {{ record.messageType === 'user' ? '用户' : 'AI' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-popconfirm
                title="确定要删除该应用的所有对话历史吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.appId!)"
              >
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
        <template #expandedRowRender="{ record }">
          <p style="margin: 0">
            {{ record.message }}
          </p>
        </template>
        <template #expandColumnTitle>
          <span>消息内容</span>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
::v-deep(.ant-table-expand-icon-col) {
  width: 90px !important;
}
</style>
