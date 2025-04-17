<template>
  <BackButton />
  <v-card
    class="mx-auto pa-12 pb-8"
    elevation="8"
    max-width="448"
    rounded="lg"
  >
    <div class="text-subtitle-1 text-medium-emphasis">Account</div>

    <v-text-field
      v-model="email"
      density="compact"
      placeholder="Email address"
      prepend-inner-icon="mdi-email-outline"
      required
      type="email"
      variant="outlined"
    />

    <v-alert
      v-if="success"
      text="If you provided a registered email address, a login link will have been sent to your email account. Please check your inbox."
      type="success"
      variant="tonal"
    />
    <v-alert
      v-else
      text="Please fill in your email address above. You will receive an email with a login link that will enable access to your data and to submit new outbreaks."
      type="info"
      variant="tonal"
    />

    <v-btn
      block
      class="my-8"
      color="primary"
      :disabled="disabled"
      prepend-icon="mdi-send"
      size="large"
      @click="send"
    >
      Send link
    </v-btn>
  </v-card>
</template>

<script lang="ts" setup>
  import BackButton from '@/components/BackButton.vue'
  import { axiosCall } from '@/plugins/api'

  const email = ref<string>()
  const sending = ref<boolean>(false)
  const success = ref<boolean | undefined>()

  const disabled: ComputedRef<boolean> = computed(() => {
    if (!email.value || !email.value.match(/.+@.+\.{1}.+/) || sending.value) {
      return true
    }
    return false
  })

  function send () {
    sending.value = true

    axiosCall({ url: 'users/login', method: 'POST', params: email.value, contentType: 'text/plain; charset=utf-8' })
      .then(() => {
        sending.value = false
        success.value = true
        email.value = ''
      })
  }
</script>
