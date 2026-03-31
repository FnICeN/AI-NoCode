// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /chatHistory/admin/deleteByAppId */
export async function deleteChatHistoryByAppId(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chatHistory/admin/deleteByAppId', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chatHistory/admin/list/page/vo */
export async function listAllChatHistory(
  body: API.ChatHistoryQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatHistory>('/chatHistory/admin/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chatHistory/app/${param0} */
export async function listChatHistoryByAppId(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listChatHistoryByAppIdParams,
  options?: { [key: string]: any }
) {
  const { appId: param0, ...queryParams } = params
  return request<API.BaseResponsePageChatHistory>(`/chatHistory/app/${param0}`, {
    method: 'GET',
    params: {
      // pageSize has a default value: 10
      pageSize: '10',
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chatHistory/save/aiMessage */
export async function saveAiMessage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.saveAIMessageParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chatHistory/save/aiMessage', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chatHistory/save/errorMessage */
export async function saveErrorMessage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.saveErrorMessageParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chatHistory/save/errorMessage', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chatHistory/save/userMessage */
export async function saveUserMessage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.saveUserMessageParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chatHistory/save/userMessage', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
