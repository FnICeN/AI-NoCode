import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: () => import('../pages/user/UserLoginPage.vue'),
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: () => import('../pages/user/UserRegisterPage.vue'),
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: () => import('../pages/admin/UserManagePage.vue'),
    },
    {
      path: '/admin/appManage',
      name: '应用管理',
      component: () => import('../pages/admin/AppManagePage.vue'),
    },
    {
      path: '/app/chat/:appId',
      name: '应用对话',
      component: () => import('../pages/app/AppChatPage.vue'),
      props: true,
    },
    // {
    //   path: '/app/edit/:appId',
    //   name: '应用编辑',
    //   component: () => import('../pages/app/AppEditPage.vue'),
    //   props: true,
    // },
  ],
})

export default router
