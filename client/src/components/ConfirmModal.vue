<template>
  <v-dialog
    v-model="dialog"
    :max-width="options.width"
    @keydown.esc="cancel"
  >
    <v-card>
      <v-toolbar
        v-if="title"
        dark
        :color="options.color"
        dense
        flat
      >
        <v-toolbar-title class="white--text">
          {{ title }}
        </v-toolbar-title>
      </v-toolbar>
      <v-card-text
        v-show="!!message"
        class="pa-4"
      >
        <span v-html="message" />
      </v-card-text>
      <v-card-actions class="pt-0">
        <v-spacer />
        <v-btn
          :color="options.color"
          text
          @click="agree"
        >
          Yes
        </v-btn>
        <v-btn
          color="grey"
          text
          @click="cancel"
        >
          Cancel
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts" setup>
const dialog = ref<boolean>(false)
const resolve = ref()
const title = ref<string>()
const message = ref<string>()
const options = ref<Options>({
  color: 'primary',
  width: 290,
})

export interface Options {
  color?: string
  width?: number
}

const open = (t: string, m: string, o: Options) => {
  dialog.value = true
  title.value = t
  message.value = m
  options.value = Object.assign({
    color: 'primary',
    width: 290,
  }, o)

  return new Promise(res => {
    resolve.value = res
  })
}

defineExpose({
  open
})

function agree () {
  resolve.value(true)
  dialog.value = false
}
function cancel () {
  resolve.value(false)
  dialog.value = false
}
</script>
