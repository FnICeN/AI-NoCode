<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { register } from '@/api/userController.ts'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const loading = ref(false)

const handleSubmit = async () => {
  if (!formState.userAccount || !formState.userPassword || !formState.checkPassword) {
    message.warning('请填写完整信息')
    return
  }

  if (formState.userPassword !== formState.checkPassword) {
    message.error('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const res = await register(formState)
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功')
      router.push('/user/login')
    } else {
      message.error(res.data.msg || '注册失败')
    }
  } catch (error: any) {
    message.error('注册失败：' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/user/login')
}
</script>

<template>
  <div class="userRegisterPage">
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <img class="logo" src="@/assets/logo.svg" alt="Logo" />
          <h2 class="title">用户注册</h2>
        </div>

        <a-form :model="formState" layout="vertical" @finish="handleSubmit">
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

          <a-form-item label="确认密码" name="checkPassword">
            <a-input-password
              v-model:value="formState.checkPassword"
              placeholder="请再次输入密码"
              size="large"
              allow-clear
            />
          </a-form-item>

          <a-form-item>
            <a-button type="primary" html-type="submit" size="large" block :loading="loading">
              注册
            </a-button>
          </a-form-item>

          <div class="login-link">
            已有账号？
            <a @click="goToLogin">立即登录</a>
          </div>
        </a-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.userRegisterPage {
  padding: 40px 0;
}
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
}
.register-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.register-header {
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

.login-link {
  text-align: center;
  color: #666;
}

.login-link a {
  color: #1890ff;
  cursor: pointer;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
