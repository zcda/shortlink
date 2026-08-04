import { createStore } from 'vuex'

// 创建一个新的 store 实例
const store = createStore({
  state() {
    return {
      domain: 'zcdada.ink:8001'
    }
  }
})

export default store
