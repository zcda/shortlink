import { getToken, getUsername } from '@/core/auth'

const BASE_URL = '/api/short-link/admin/v1'

/**
 * 通过 SSE 发送 Agent 聊天消息，流式返回。
 * @param {string} message - 用户消息
 * @param {string} sessionId - 会话 ID
 * @param {function} onChunk - 每收到一段文本时回调
 * @param {function} onDone - 流结束时回调
 * @param {function} onError - 出错时回调
 */
export function chatStream(message, sessionId, onChunk, onDone, onError) {
  const token = getToken()
  const username = getUsername()

  fetch(`${BASE_URL}/agent/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Token': token || '',
      'Username': username || ''
    },
    body: JSON.stringify({ message, sessionId })
  }).then(async (response) => {
    if (!response.ok) {
      onError(new Error(`HTTP ${response.status}`))
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data === '[DONE]') {
            onDone()
            return
          }
          onChunk(data)
        } else if (line.startsWith('event:done')) {
          onDone()
          return
        }
      }
    }
    onDone()
  }).catch((err) => {
    onError(err)
  })
}
