<template>
  <v-app>
    <v-main class="d-flex flex-column">
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
          <v-btn
            v-if="store.token && store.token.token && store.token.user && store.token.user.isAdmin"
            icon="mdi-account-details"
            to="/user"
          />

          <v-tooltip v-if="userInitials" :text="store.token?.user?.userName">
            <template v-slot:activator="{ props }">
              <v-avatar v-bind="props" class="mx-2" color="primary">
                <span class="text-h6">{{ userInitials }}</span>
              </v-avatar>
            </template>
          </v-tooltip>
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

      <v-container class="app-content">
        <v-alert
          v-model="logoutWarning"
          closable
          icon="mdi-logout"
          title="Session expired"
          type="warning"
          variant="tonal"
        >
          <template #text>
            Your session has expired. Please <router-link @click="logoutWarning = false" to="/login">sign back in</router-link> to continue.
          </template>
        </v-alert>

        <router-view />
      </v-container>

      <v-footer
        class="d-flex align-center justify-center ga-2 flex-wrap flex-grow-1 py-3 mt-auto"
        color="surface-light"
      >
        <v-btn
          v-for="link in links"
          :key="link.text"
          :href="link.href"
          rounded
          :title="link.tooltip"
          :to="link.to"
          variant="text"
          @click="link.click"
        >
          <v-icon
            v-if="link.icon"
            :icon="link.icon"
          />
          <span v-if="link.text">{{ link.text }}</span>
        </v-btn>

        <div class="flex-1-0-100 text-center mt-2">
          {{ new Date().getFullYear() }} — <strong><a href="https://ics.hutton.ac.uk/">Information &amp; Computational Sciences</a> — <a href="https://www.hutton.ac.uk/">The James Hutton Institute</a></strong>
        </div>
      </v-footer>
    </v-main>

    <v-overlay
      v-model="loading"
      persistent
      class="align-center justify-center"
    >
      <v-progress-circular
        color="primary"
        size="64"
        indeterminate
      />
    </v-overlay>

    <ConfirmModal ref="confirmModal" />
  </v-app>
</template>

<script lang="ts" setup>
  import { axiosCall } from '@/plugins/api'
  import { coreStore } from '@/stores/app'
  import { useRoute, useRouter } from 'vue-router'
  import { useGoTo } from 'vuetify'
  import { usePlausible } from 'v-plausible/vue'
  import type { User } from '@/plugins/types/User'
  // @ts-ignore
  import emitter from 'tiny-emitter/instance'
  const { trackEvent } = usePlausible()

  type CallbackFunction = () => void

  interface Link {
    id: string
    text?: string
    to?: string
    href?: string
    icon?: string
    tooltip: string
    click?: CallbackFunction
  }

  const links = ref<Link[]>([
    { id:'home', tooltip: 'Go home', text: 'Home', to: '/' },
    { id: 'tos', tooltip: 'Privacy Policy & Terms Of Use', text: 'Privacy Policy & Terms Of Use', href: 'https://www.hutton.ac.uk/terms' },
    { id: 'totop', tooltip: 'Return to top', icon: 'mdi-chevron-up', click: () => { goTo(0) } }
  ])

  const route = useRoute()
  const router = useRouter()
  const store = coreStore()
  const goTo = useGoTo()

  const confirmModal = ref()
  const loading = ref<boolean>(false)
  const logoutWarning = ref<boolean>(false)

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
      // Take the token, then remove from URL
      // @ts-ignore
      store.setToken(newToken)
      router.replace({ query: {} })
      nextTick(() => {
        axiosCall<User>({ url: 'users/status' })
          .then((r: User) => {
            logoutWarning.value = false
            store.setUser(r)
          })
          .catch(e => {
            if (e.status === 403) {
              store.setToken(null)
              logoutWarning.value = true
            }
          })
      })
    }
  })

  const userInitials: ComputedRef<string | undefined> = computed(() => {
    if (store.token && store.token.user && store.token.user.userName) {
      return store.token.user.userName.split(/\s+/g).slice(0, 2).map(s => s.charAt(0).toUpperCase()).join('')
    } else {
      return undefined
    }
  })

  axiosCall<User>({ url: 'users/status' })
    .then((r: User) => {
      logoutWarning.value = false
      store.setUser(r)
    })
    .catch(e => {
      if (e.status === 403) {
        store.setToken(null)
        logoutWarning.value = true
      }
    })
  
  function logout () {
    confirmModal.value.open(undefined, 'Log out of Fight Against Blight?', { color: 'error' })
      .then((response: boolean) => {
        if (response) {
          store.setToken(null)
        }
      })
  }

  function setLoading (newValue: boolean) {
    loading.value = newValue
  }

  function plausibleEvent (data: any) {
    if (data) {
      if (data.props) {
        trackEvent(data.key, { props: data.props })
      } else {
        trackEvent(data.key)
      }
    }
  }

  onMounted(() => {
    emitter.on('set-loading', setLoading)
    emitter.on('plausible-event', plausibleEvent)
  })
  onBeforeUnmount(() => {
    emitter.off('set-loading', setLoading)
    emitter.off('plausible-event', plausibleEvent)
  })
</script>

<style>
#logo img {
  width: auto;
}
.app-content {
  height: 100%;
  flex-grow: 1;
}
</style>
