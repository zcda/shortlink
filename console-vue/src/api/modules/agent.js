import { getToken, getUsername } from '@/core/auth'

const BASE_URL = '/api/short-link/admin/v1'

/**
 * 通过 SSE 发送 Agent 聊天消息，流式返回。
 * @param {string} message - 用户消息
 * @param {string} sessionId - 会话 ID
 * @param {string} model - 指定模型 id：auto/deepseek/qwen/ollama；缺省为 auto
 * @param {function} onChunk - 每收到一段文本时回调
 * @param {function} onDone - 流结束时回调 (meta: { traceId })
 * @param {function} onError - 出错时回调
 * @param {function} onMeta - meta 事件回调（traceId 等）
 */
export function chatStream(message, sessionId, model, onChunk, onDone, onError, onMeta) {
  const token = getToken()
  const username = getUsername()

  fetch(`${BASE_URL}/agent/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Token': token || '',
      'Username': username || ''
    },
    body: JSON.stringify({ message, sessionId, model: model || 'auto' })
  }).then(async (response) => {
    if (!response.ok) {
      onError(new Error(`HTTP ${response.status}`))
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let eventType = null            // 当前事件类型(event: xxx)
    let dataLines = []              // 当前事件的 data 行
    let finished = false            // 是否已收到 [DONE]

    // 按 SSE 规范：一个事件里多行 data: 应以 \n 拼接成一个数据；空行 = 事件结束
    const flushEvent = () => {
      if (dataLines.length === 0) {
        eventType = null
        return
      }
      const data = dataLines.join('\n')
      dataLines = []
      if (eventType === 'meta') {
        try {
          const meta = JSON.parse(data)
          if (onMeta) onMeta(meta)
        } catch (e) { /* ignore parse error */ }
      } else if (eventType === 'error') {
        if (onError) onError(new Error(data))
      } else if (data === '[DONE]') {
        finished = true
        onDone()
      } else if (data === '') {
        onChunk('\n')
      } else {
        onChunk(data)
      }
      eventType = null
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      // 兼容 \r\n 与 \n 行分隔
      const lines = buffer.split(/\r?\n/)
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line === '') {
          flushEvent()                                    // 空行结束当前事件
        } else if (line.startsWith('event:')) {
          eventType = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          // 本后端（Spring MVC SseEmitter）编码为 "data:" + 内容，冒号后【没有】分隔空格；
          // 内容里的真实空格（如 "1 个分组"、"| 短链接 |"）必须原样保留。
          // 若按 SSE 规范盲目剥掉一个前导空格，空格 token 会被当成空行→换行，表格/正文全被拆碎。
          dataLines.push(line.slice(5))
        }
        // id:/retry:/:注释 等字段对当前功能无用，忽略
      }
    }
    flushEvent()   // 流结束兜底（若服务端直接断开而没有 [DONE]）
    if (!finished) onDone()
  }).catch((err) => {
    onError(err)
  })
}

/**
 * 获取可选模型列表（含熔断状态），渲染"模型选择"下拉
 * 返回形如 [{ id, name, model, state }]，state: CLOSED/OPEN/HALF_OPEN
 */
export function fetchModels() {
  const token = getToken()
  const username = getUsername()
  return fetch(`${BASE_URL}/agent/models`, {
    headers: { 'Token': token || '', 'Username': username || '' }
  }).then(r => r.json()).catch(() => [])
}

/**
 * 获取最近调用追踪记录
 */
export function fetchTraces() {
  const token = getToken()
  const username = getUsername()
  return fetch(`${BASE_URL}/agent/traces`, {
    headers: { 'Token': token || '', 'Username': username || '' }
  }).then(r => r.json()).catch(() => [])
}

/**
 * 获取 Agent 健康状态
 */
export function fetchAgentHealth() {
  const token = getToken()
  const username = getUsername()
  return fetch(`${BASE_URL}/agent/health`, {
    headers: { 'Token': token || '', 'Username': username || '' }
  }).then(r => r.json()).catch(() => ({}))
}
