<template>
  <div class="ai-chat-container">
    <div class="chat-header">
      <span class="chat-title">AI 智能助手</span>
      <el-tag size="small" type="info">Powered by DeepSeek</el-tag>
    </div>

    <div class="chat-body" ref="chatBody">
      <div v-if="messages.length === 0" class="chat-placeholder">
        <p>你好！我是短链接管理助手，可以帮你：</p>
        <ul>
          <li>创建短链接（将长 URL 转为短链接）</li>
          <li>查看分组列表和短链接列表</li>
          <li>查询访问统计数据</li>
          <li>管理回收站</li>
        </ul>
        <p>直接输入你想做的事情吧～</p>
      </div>

      <div v-for="(msg, idx) in messages" :key="idx" class="message-row"
        :class="msg.role === 'user' ? 'msg-user' : 'msg-assistant'">
        <div class="msg-bubble">{{ msg.content }}</div>
        <div v-if="msg.role === 'assistant' && msg.streaming" class="cursor-blink">|</div>
      </div>

      <div v-if="loading && currentAssistantMsg === ''" class="msg-assistant">
        <div class="msg-bubble typing-indicator">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <div class="chat-footer">
      <el-input
        v-model="inputText"
        placeholder="输入消息，如：帮我创建一个短链接 https://example.com"
        @keyup.enter="sendMessage"
        :disabled="loading"
        class="chat-input"
      />
      <el-button type="primary" @click="sendMessage" :disabled="loading || !inputText.trim()">
        发送
      </el-button>
      <el-button @click="clearChat" :disabled="loading">清空</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { chatStream } from '@/api/modules/agent'
import { ElMessage } from 'element-plus'

const SESSION_KEY = 'ai-agent-session'
const HISTORY_KEY = 'ai-agent-chat-history'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const currentAssistantMsg = ref('')
const chatBody = ref(null)
const sessionId = ref(localStorage.getItem(SESSION_KEY) || '')

function generateSessionId() {
  return 'session-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
}

function saveHistory() {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(messages.value.slice(-100)))
  } catch (e) {
    // localStorage 空间不足时忽略，不影响对话
  }
}

onMounted(() => {
  if (!sessionId.value) {
    sessionId.value = generateSessionId()
    localStorage.setItem(SESSION_KEY, sessionId.value)
  }
  const history = localStorage.getItem(HISTORY_KEY)
  if (history) {
    try {
      messages.value = JSON.parse(history).map(m => ({ ...m, streaming: false }))
    } catch (e) {
      // 历史数据损坏时忽略
    }
  }
})

watch(messages, saveHistory, { deep: true })

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  currentAssistantMsg.value = ''
  loading.value = true
  await scrollToBottom()

  chatStream(
    text,
    sessionId.value,
    // onChunk
    (chunk) => {
      currentAssistantMsg.value += chunk
      // 更新或新增助手消息
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg && lastMsg.role === 'assistant' && lastMsg.streaming) {
        lastMsg.content = currentAssistantMsg.value
      } else {
        messages.value.push({ role: 'assistant', content: currentAssistantMsg.value, streaming: true })
      }
      scrollToBottom()
    },
    // onDone
    () => {
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg && lastMsg.role === 'assistant') {
        lastMsg.streaming = false
      }
      loading.value = false
      currentAssistantMsg.value = ''
    },
    // onError
    (err) => {
      console.error('Agent chat error:', err)
      ElMessage.error('请求失败，请稍后重试')
      loading.value = false
      currentAssistantMsg.value = ''
    }
  )
}

function clearChat() {
  messages.value = []
  sessionId.value = generateSessionId()
  localStorage.setItem(SESSION_KEY, sessionId.value)
  localStorage.removeItem(HISTORY_KEY)
}

async function scrollToBottom() {
  await nextTick()
  if (chatBody.value) {
    chatBody.value.scrollTop = chatBody.value.scrollHeight
  }
}
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.chat-placeholder {
  text-align: center;
  color: #909399;
  margin-top: 60px;
}

.chat-placeholder ul {
  display: inline-block;
  text-align: left;
  margin: 12px 0;
  padding-left: 24px;
}

.chat-placeholder li {
  margin: 4px 0;
}

.message-row {
  margin-bottom: 16px;
  display: flex;
}

.msg-user {
  justify-content: flex-end;
}

.msg-user .msg-bubble {
  background: #409eff;
  color: #fff;
  border-radius: 12px 12px 4px 12px;
  max-width: 70%;
}

.msg-assistant .msg-bubble {
  background: #f0f0f0;
  color: #303133;
  border-radius: 12px 12px 12px 4px;
  max-width: 85%;
}

.msg-bubble {
  padding: 10px 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.cursor-blink {
  color: #409eff;
  font-size: 16px;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 14px 16px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #909399;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.chat-footer {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid #eee;
  background: #fafafa;
}

.chat-input {
  flex: 1;
}
</style>
