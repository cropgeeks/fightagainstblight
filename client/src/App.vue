<template>
  <v-app>
    <v-main>
      <v-app-bar :elevation="2">
        <v-img
          id="logo"
          class="ms-4"
          src="@/assets/FAB-logo.png"
          max-height="40"
          style="cursor: pointer"
          @click="router.push('/')"
        />

        <template #append>
          <span
            v-if="store.token && store.token.user && store.token.user.userName"
            class="me-2"
          >
            {{ store.token.user.userName }}
          </span>

          <v-btn
            v-if="store.token && store.token.token"
            icon="mdi-logout"
            @click="logout"
          />
          <v-btn
            v-else
            icon="mdi-login"
            to="/login"
          />
        </template>
      </v-app-bar>

      <v-container>
        <router-view />
      </v-container>
    </v-main>
  </v-app>
</template>

<script lang="ts" setup>
  import { axiosCall } from '@/plugins/api'
  import { coreStore } from '@/stores/app'
  import { useRoute, useRouter } from 'vue-router'
  import type { User } from '@/plugins/types/User'

  const route = useRoute()
  const router = useRouter()
  const store = coreStore()

  // Set base URL based on environment
  let baseUrl = './api/'
  if (import.meta.env.VITE_BASE_URL) {
    baseUrl = import.meta.env.VITE_BASE_URL
  }
  store.setBaseUrl(baseUrl)

  // TODO: Remove
  // store.setToken('test')
  // store.setToken(null)

  watch(() => route.query.token, async newToken => {
    if (newToken) {
      console.log(newToken)
      // Take the token, then remove from URL
      // @ts-ignore
      store.setToken(newToken)
      router.replace({ query: {} })
      nextTick(() => {
        axiosCall<User>({ url: 'users/status' })
          .then((r: User) => {
            store.setUser(r)
          })
      })
    }
  })

  axiosCall<User>({ url: 'users/status' })
    .then((r: User) => {
      store.setUser(r)
    })
  
  function logout () {
    store.setToken(null)
  }
</script>

<style>
#logo img {
  width: auto;
}
</style>
