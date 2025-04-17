/**
 * plugins/index.ts
 *
 * Automatically included in `./src/main.ts`
 */

// Plugins
import vuetify from './vuetify'
import pinia from '../stores'
import router from '../router'

// Types
import type { App } from 'vue'
import { createPlausible } from 'v-plausible/vue'

export function registerPlugins (app: App) {
  app
    .use(vuetify)
    .use(router)
    .use(pinia)
    .use(createPlausible({
      init: {
        domain: 'blight.hutton.ac.uk',
        hashMode: true,
        apiHost: 'https://plausible.hutton.ac.uk',
        trackLocalhost: false
      },
      settings: {
        enableAutoPageviews: true
      }
    }))
}
