<template>
  <div class="ai-chat-container">
    <div class="chat-header">
      <span class="chat-title">AI 智能助手</span>
      <el-tag size="small" :type="modelStatus === 'degraded' ? 'warning' : 'success'" effect="plain">
        {{ modelStatus === 'degraded' ? '备选模型' : 'DeepSeek 主模型' }}
      </el-tag>
      <el-tooltip content="多模型路由 + 熔断保护" placement="bottom">
        <el-tag size="small" type="info" effect="plain" style="cursor: help">🛡 多模型路由</el-tag>
      </el-tooltip>
      <div style="flex:1"></div>
      <el-button size="small" text @click="showTraces = !showTraces">
        {{ showTraces ? '隐藏' : '调用记录' }} ({{ traces.length }})
      </el-button>
    </div>

    <div class="chat-body" ref="chatBody" :class="{ 'with-panel': showTraces }">
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
        <div v-if="msg.role === 'user'" class="msg-bubble">{{ msg.content }}</div>
        <div v-else class="msg-bubble markdown-body" v-html="renderMd(msg)" />
        <div v-if="msg.role === 'assistant' && msg.streaming" class="cursor-blink">|</div>
      </div>

      <div v-if="loading && currentAssistantMsg === ''" class="msg-assistant">
        <div class="msg-bubble typing-indicator">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <transition name="slide">
      <div v-if="showTraces" class="trace-panel">
        <div class="trace-panel-header">
          <span>调用追踪（最近 20 条）</span>
          <el-button size="small" text @click="refreshTraces">刷新</el-button>
        </div>
        <div class="trace-list">
          <div v-if="traces.length === 0" class="trace-empty">暂无调用记录</div>
          <div v-for="(t, i) in traces" :key="i" class="trace-item"
            :class="{ 'trace-fail': !t.success }">
            <span class="trace-id">#{{ t.traceId }}</span>
            <span class="trace-duration">{{ t.durationMs }}ms</span>
            <el-tag size="small" :type="t.success ? 'success' : 'danger'" effect="plain">
              {{ t.success ? '成功' : '失败' }}
            </el-tag>
            <span v-if="t.errorMsg" class="trace-error">{{ t.errorMsg }}</span>
          </div>
        </div>
      </div>
    </transition>

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
import { chatStream, fetchTraces } from '@/api/modules/agent'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'

marked.setOptions({
  breaks: true,
  gfm: true
})

const SESSION_KEY = 'ai-agent-session'
const HISTORY_KEY = 'ai-agent-chat-history'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const currentAssistantMsg = ref('')
const chatBody = ref(null)
const sessionId = ref(localStorage.getItem(SESSION_KEY) || '')
const showTraces = ref(false)
const traces = ref([])
const modelStatus = ref('primary')

function generateSessionId() {
  return 'session-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
}

function saveHistory() {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(messages.value.slice(-100)))
  } catch (e) {}
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
    } catch (e) {}
  }
  refreshTraces()
})

watch(messages, saveHistory, { deep: true })

async function refreshTraces() {
  try {
    traces.value = await fetchTraces()
  } catch (e) {}
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  currentAssistantMsg.value = ''
  loading.value = true
  await scrollToBottom()

  const startTime = Date.now()
  let currentTraceId = ''

  chatStream(
    text,
    sessionId.value,
    // onChunk
    (chunk) => {
      currentAssistantMsg.value += chunk
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
      const duration = Date.now() - startTime
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg && lastMsg.role === 'assistant') {
        lastMsg.streaming = false
        lastMsg.durationMs = duration
        lastMsg.traceId = currentTraceId
      }
      loading.value = false
      currentAssistantMsg.value = ''
      refreshTraces()
    },
    // onError
    (err) => {
      console.error('Agent chat error:', err)
      modelStatus.value = 'degraded'
      ElMessage.error('请求失败，请稍后重试')
      loading.value = false
      currentAssistantMsg.value = ''
      refreshTraces()
    },
    // onMeta
    (meta) => {
      currentTraceId = meta.traceId || ''
    }
  )
}

function renderMd(msg) {
  if (!msg.content) return ''
  if (msg.streaming) return msg.content.replace(/\n/g, '<br>')
  const html = marked.parse(msg.content)
  const meta = msg.durationMs ? `<div class="msg-meta">${msg.durationMs}ms · ${msg.traceId || ''}</div>` : ''
  return html + meta
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
  gap: 8px;
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
  transition: max-height 0.3s;
}
.chat-body.with-panel {
  max-height: calc(100% - 320px);
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

.msg-meta {
  margin-top: 6px;
  font-size: 11px;
  color: #909399;
  border-top: 1px solid #e0e0e0;
  padding-top: 4px;
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

.trace-panel {
  border-top: 1px solid #eee;
  background: #fafafa;
  max-height: 200px;
  overflow-y: auto;
}

.trace-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  position: sticky;
  top: 0;
  background: #fafafa;
}

.trace-list {
  padding: 0 16px 8px;
}

.trace-empty {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 16px;
}

.trace-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.trace-item:last-child {
  border-bottom: none;
}

.trace-fail {
  background: #fef0f0;
  margin: 0 -12px;
  padding: 6px 12px;
  border-radius: 4px;
}

.trace-id {
  color: #409eff;
  font-family: monospace;
  min-width: 70px;
}

.trace-duration {
  color: #606266;
  font-family: monospace;
  min-width: 60px;
}

.trace-error {
  color: #f56c6c;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slide-enter-active, .slide-leave-active {
  transition: all 0.3s;
}
.slide-enter-from, .slide-leave-to {
  max-height: 0;
  opacity: 0;
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

/* markdown 渲染样式 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 8px 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.markdown-body :deep(p) { margin: 4px 0; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 4px 0; padding-left: 20px; }
.markdown-body :deep(li) { margin: 2px 0; }
.markdown-body :deep(strong) { font-weight: 600; color: #303133; }
.markdown-body :deep(code) {
  background: #e8e8e8;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 13px;
  font-family: monospace;
}
.markdown-body :deep(pre) {
  background: #f5f5f5;
  padding: 10px 14px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 6px 0;
  font-size: 13px;
}
.markdown-body :deep(pre code) { background: none; padding: 0; }
.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  font-size: 13px;
}
.markdown-body :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  text-align: left;
}
.markdown-body :deep(td) {
  padding: 5px 10px;
  border: 1px solid #e0e0e0;
}
.markdown-body :deep(tr:nth-child(even)) { background: #fafafa; }
.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 10px 0;
}
.markdown-body :deep(blockquote) {
  border-left: 3px solid #409eff;
  padding: 4px 12px;
  margin: 6px 0;
  color: #606266;
  background: #f5f7fa;
  border-radius: 0 4px 4px 0;
}
.markdown-body :deep(a) { color: #409eff; }
</style>
