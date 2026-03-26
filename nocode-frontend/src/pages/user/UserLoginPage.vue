<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { login } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const loading = ref(false)

const handleSubmit = async () => {
  if (!formState.userAccount || !formState.userPassword) {
    message.warning('请输入用户账号和密码')
    return
  }

  loading.value = true
  try {
    const res = await login(formState)
    if (res.data.code === 0 && res.data.data) {
      message.success('登录成功')
      loginUserStore.setLoginUser(res.data.data)
      router.push('/')
    } else {
      message.error(res.data.msg || '登录失败')
    }
  } catch (error: any) {
    message.error('登录失败：' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/user/register')
}
</script>

<template>
  <div class="userLoginPage">
    <div class="login-card">
      <div class="login-header">
        <img class="logo" src="@/assets/logo.svg" alt="Logo" />
        <h2 class="title">用户登录</h2>
      </div>

      <a-form
        :model="formState"
        layout="vertical"
        @finish="handleSubmit"
      >
        <a-form-item label="用户账号" name="userAccount">
          <a-input
            v-model:value="formState.userAccount"
            placeholder="请输入用户账号"
            size="large"
            allow-clear
          />
        </a-form-item>

        <a-form-item label="密码" name="userPassword">
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="请输入密码"
            size="large"
            allow-clear
          />
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="loading"
          >
            登录
          </a-button>
        </a-form-item>

        <div class="register-link">
          还没有账号？
          <a @click="goToRegister">立即注册</a>
        </div>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
}

.title {
  margin: 0;
  font-size: 24px;
  color: #333;
  font-weight: 600;
}

.register-link {
  text-align: center;
  color: #666;
}

.register-link a {
  color: #1890ff;
  cursor: pointer;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
