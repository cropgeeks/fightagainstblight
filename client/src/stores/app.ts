// Utilities
import type { User } from '@/plugins/types/User'
import { defineStore } from 'pinia'

export interface Token {
  token: string | null
  user?: User
}

export const coreStore = defineStore('fab', {
  state: () => ({
    baseUrl: './api/' as string,
    token: null as Token | null,
  }),
  actions: {
    setBaseUrl (newBaseUrl: string) {
      this.baseUrl = newBaseUrl
    },
    setToken (newToken: string | null) {
      if (this.token) {
        this.token.token = newToken
        delete this.token.user
      } else {
        this.token = {
          token: newToken,
        }
      }
    },
    setUser (newUser: User) {
      if (this.token) {
        this.token.user = newUser
      } else {
        this.token = {
          token: '',
          user: newUser
        }
      }
    }
  },
  persist: {
    key: 'fab',
  },
})
