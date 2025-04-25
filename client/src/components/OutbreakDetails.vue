<template>
  <v-list>
    <v-list-item
      title="Outbreak code"
      :subtitle="props.outbreak.outbreakCode"
    />
    <v-list-item
      title="Reported on"
      :subtitle="props.outbreak.dateSubmitted ? new Date(props.outbreak.dateSubmitted).toLocaleDateString() : 'N/A'"
    />
    <v-list-item
      title="Status">
      <template #subtitle>
        <v-chip
          v-if="props.outbreak.status"
          :color="status.get(props.outbreak.status)?.color"
          :prepend-icon="status.get(props.outbreak.status)?.icon"
        >
          {{ status.get(props.outbreak.status)?.text }}
        </v-chip>
      </template>
    </v-list-item>
    <v-list-item
      title="Severity">
      <template #subtitle>
        <v-chip v-if="props.outbreak.severityName">
          <v-img
            class="me-3"
            contains
            height="20"
            :src="`/img/severity/${props.outbreak.severityName.toLowerCase().replace(/[\s\/]+/g, '-')}.svg`"
            width="20"
          />
          <span>{{ props.outbreak.severityName }}</span>
        </v-chip>
      </template>
    </v-list-item>
    <v-list-item
      title="Source">
      <template #subtitle>
        <v-chip v-if="props.outbreak.sourceName">
          <v-img
            class="me-3"
            contains
            height="20"
            :src="`/img/source/${props.outbreak.sourceName.toLowerCase().replace(/[\s\/]+/g, '-')}.svg`"
            width="20"
          />
          <span>{{ props.outbreak.sourceName }}</span>
        </v-chip>
      </template>
    </v-list-item>
    <v-list-item
      title="Host">
      <template #subtitle>
        <v-chip
          v-if="props.outbreak.host"
          :color="host.get(props.outbreak.host)?.color"
        >
          <v-img
            class="me-3"
            contains
            height="20"
            :src="`/img/host/${props.outbreak.host.toLowerCase().replace(/[\s\/]+/g, '-')}.svg`"
            width="20"
          />
          <span>{{ host.get(props.outbreak.host)?.text }}</span>
        </v-chip>
      </template>
    </v-list-item>
  </v-list>
</template>

<script lang="ts" setup>
  import type { Outbreak } from '@/plugins/types/Outbreak'
  import { outbreakStatus, outbreakHosts, type Host, type Status } from '@/plugins/constants'

  interface Props {
    outbreak: Outbreak,
  }

  const props = withDefaults(defineProps<Props>(), {
    outbreak: undefined,
  })

  const status = ref<Map<string, Status>>(outbreakStatus)
  const host = ref<Map<string, Host>>(outbreakHosts)
</script>
