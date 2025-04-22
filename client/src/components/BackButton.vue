<template>
  <v-btn
    class="mb-12"
    color="primary"
    prepend-icon="mdi-chevron-left"
    :to="props.to"
    variant="tonal"
  >
    {{ props.text }}
  </v-btn>
</template>

<script lang="ts" setup>
  interface Props {
    to?: string,
    text?: string,
  }

  const props = withDefaults(defineProps<Props>(), {
    to: '/',
    text: 'Back home',
  })
</script>
