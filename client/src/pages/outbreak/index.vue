<template>
  <BackButton />

  <v-row>
    <!-- Stuff along the top -->
    <v-col :cols=12 :md=8>
      <h1 class="mb-4">Blight Report & Incident Summary</h1>
      <p class="mb-2">Below you'll find a table and a map showing outbreaks along with their status and reporting date. Use the filtering options to search for outbreaks of interest or use the map to browse outbreaks for a specific region.</p>
      <p class="mb-2">To report a new outbreak, please use the form on <router-link to="/submit">this page</router-link>.</p>
    </v-col>
    <v-col :cols=12 :md=4 class="d-flex align-center pa-5 sponsor-img">
      <!-- Hutton logos, hidden or right aligned depending on screen size -->
      <v-img class="d-none d-md-inline-block" position="right center" src="@/assets/hutton.svg" />
      <v-img class="d-md-none" position="center center" src="@/assets/hutton.svg" />
    </v-col>
  </v-row>

  <v-row class="mb-4">
    <v-col
      :cols="12"
      :lg="3"
      :md="4"
      :sm="6"
    >
      <!-- YEAR -->
      <v-select
        v-model="selectedYear"
        autocomplete="off"
        hide-details
        label="Year"
        :items="years"
      />
    </v-col>
    <v-col
      :cols="12"
      :lg="3"
      :md="4"
      :sm="6"
    >
      <!-- OUTBREAK STATUS -->
      <v-autocomplete
        v-model="selectedStatus"
        autocomplete="off"
        clearable
        hide-details
        label="Status"
        :items="statusOptions"
      />
    </v-col>
    <v-col
      :cols="12"
      :lg="3"
      :md="4"
      :sm="6"
    >
      <!-- VARIETY -->
      <v-autocomplete
        v-model="selectedVariety"
        autocomplete="off"
        clearable
        hide-details
        label="Variety"
        :items="varietyOptions"
      />
    </v-col>
    <v-col
      :cols="12"
      :lg="3"
      :md="4"
      :sm="6"
    >
      <!-- SEVERITY -->
      <v-autocomplete
        v-model="selectedSeverity"
        autocomplete="off"
        clearable
        hide-details
        label="Severity"
        :items="severityOptions"
      />
    </v-col>
    <v-col
      :cols="12"
      :lg="3"
      :md="4"
      :sm="6"
    >
      <!-- SOURCE -->
      <v-autocomplete
        v-model="selectedSource"
        autocomplete="off"
        clearable
        hide-details
        label="Source"
        :items="sourceOptions"
      />
    </v-col>
    <v-col
      :cols="12"
      :lg="3"
      :md="4"
      :sm="6"
    >
      <!-- POSTCODE -->
      <v-text-field
        v-model="postcodeTemp"
        autocomplete="off"
        clearable
        :error="postcodeValid === false"
        :error-messages="postcodeValid === false ? ['Invalid post outcode.'] : null"
        label="Search for postcode district (e.g. DD2)"
        @blur="setPostcode"
        @keydown.enter.exact="$event.target.blur()"
      />
    </v-col>
    <v-col
      :cols="12"
      :lg="3"
      :md="4"
      :sm="6"
    >
      <!-- OUTCODE -->
      <v-text-field
        v-model="outbreakCodeTemp"
        clearable
        label="Search for outbreak code"
        @blur="setOutbreakCode"
        @keydown.enter.exact="$event.target.blur()"
      />
    </v-col>
    <v-col
      v-if="store.token && store.token.user"
      :cols="12"
      :lg="3"
      :md="4"
      :sm="6"
    >
      <!-- ONLY OWN DATA -->
      <v-checkbox
        v-model="onlyShowUserData"
        label="Only show my data"
      />
    </v-col>
  </v-row>
  <v-row
    class="mb-3"
  >
    <v-col
      v-if="isAdmin"
      :cols="12"
      :lg="3"
      :md="4"
      :sm="6"
    >
      <!-- DOWNLOAD REPORT -->
      <v-btn
        class="mb-2"
        prepend-icon="mdi-download"
        text="Download report"
        @click="downloadReport"
      />
      <p class="text-caption text-info">The download will contain all outbreaks in your current filtering.</p>
    </v-col>
  </v-row>

  <!-- OUTBREAK TABLE -->
  <v-data-table
    class="mb-4"
    :headers="headers"
    :items="outbreaks"
    :loading="loading"
    :page="page"
    :sort-by="[{ key: 'outbreakCode', order: 'desc' }]"
    @update:page="p => { page = p }"
  >
    <template #item.status="{ value }">
      <!-- CHIP FOR THE STATUS -->
      <v-chip
        v-if="value"
        :color="status.get(value)?.color"
        :prepend-icon="status.get(value)?.icon"
      >
        {{ status.get(value)?.text }}
      </v-chip>
    </template>

    <template #item.host="{ value }">
      <!-- CHIP FOR THE HOST -->
      <v-chip
        v-if="value"
        :color="host.get(value)?.color"
      >
        <v-img
          class="me-3"
          contains
          height="20"
          :src="`/img/host/${value.toLowerCase().replaceAll(/[\s\/]+/g, '-')}.svg`"
          width="20"
        />
        {{ host.get(value)?.text }}
      </v-chip>
    </template>

    <template #item.outbreakCode="{ value, item }">
      <!-- OUTBREAK DETAILS BUTTON -->
      <v-btn
        v-if="store.token"
        block
        :color="(currentUserId && (item.userId === currentUserId)) ? 'secondary' : 'primary'"
        :to="`/outbreak/${item.outbreakId}`"
      >
        {{ value }}
      </v-btn>
      <span v-else>{{ value }}</span>
    </template>

    <template #item.severityName="{ value }">
      <!-- CHIP FOR THE SEVERITY -->
      <v-chip v-if="value">
        <v-img
          class="me-3"
          contains
          height="20"
          :src="`/img/severity/${value.toLowerCase().replaceAll(/[\s\/]+/g, '-')}.svg`"
          width="20"
        />
        <span>{{ value }}</span>
      </v-chip>
    </template>
    <template #item.sourceName="{ value }">
      <!-- CHIP FOR THE SOURCE -->
      <v-chip v-if="value">
        <v-img
          class="me-3"
          contains
          height="20"
          :src="`/img/source/${value.toLowerCase().replaceAll(/[\s\/]+/g, '-')}.svg`"
          width="20"
        />
        <span>{{ value }}</span>
      </v-chip>
    </template>
  </v-data-table>

  <!-- Outbreak map -->
  <OutbreakMap :outbreaks="outbreaksWithGps" />

  <!-- Contact information -->
  <v-alert
    class="mt-5"
    icon="mdi-help-circle"
    title="Contact us"
    type="info"
    variant="tonal"
  >
    <template #text>
      If you wish more information about the service or encounter any issues, please contact us at <a href="mailto:fab@hutton.ac.uk?subject=Question from blight.hutton.ac.uk">fab@hutton.ac.uk</a>.
    </template>
  </v-alert>

  <!-- Sponsors -->
  <Sponsors class="my-5" />
</template>

<script lang="ts" setup>
  import * as XLSX from 'xlsx'
  import axios from 'axios'
  import OutbreakMap from '@/components/OutbreakMap.vue'
  import Sponsors from '@/components/Sponsors.vue'
  import BackButton from '@/components/BackButton.vue'
  import { axiosCall } from '@/plugins/api'
  import { outbreakStatus, outbreakHosts } from '@/plugins/constants'
  import type { Host, Status } from '@/plugins/constants'
  import type { Outbreak } from '@/plugins/types/Outbreak'
  import type { SelectOption } from '@/plugins/types/SelectOption'
  import type { Severity } from '@/plugins/types/Severity'
  import type { Source } from '@/plugins/types/Source'
  import type { Variety } from '@/plugins/types/Variety'
  import { coreStore } from '@/stores/app'
  // @ts-ignore
  import emitter from 'tiny-emitter/instance'

  // COMPOSITION
  const store = coreStore()
  const router = useRouter()

  // REFS
  // Database values
  const outbreaks = ref<Outbreak[]>([])
  const severities = ref<Severity[]>([])
  const varieties = ref<Variety[]>([])
  const sources = ref<Source[]>([])
  const years = ref<number[]>([])

  // Convenience stuff
  const loading = ref<boolean>(false)
  const postcodeValid = ref<boolean | undefined>()
  const page = ref<number | undefined>(1)
  const forcePage = ref<number | undefined>(1)
  
  // User input
  const onlyShowUserData = ref<boolean>(false)
  const outbreakCodeTemp = ref<string | undefined>()
  const outbreakCode = ref<string | undefined>()
  const postcodeTemp = ref<string | undefined>()
  const postcode = ref<string | undefined>()
  
  // User selections
  const selectedSource = ref<number>()
  const selectedSeverity = ref<number>()
  const selectedVariety = ref<number>()
  const selectedYear = ref<number | undefined>()
  const selectedStatus = ref<string | undefined>()
  
  // UI helpers
  const headers = ref<any[]>([
    { title: 'Outbreak code', key: 'outbreakCode' },
    { title: 'Severity', key: 'severityName' },
    { title: 'Source', key: 'sourceName' },
    { title: 'Status', key: 'status' },
    { title: 'Host', key: 'host' },
    { title: 'Reported on', key: 'dateSubmitted', sortRaw: sort('dateSubmitted'), value: (item: Outbreak) => (item && item.dateSubmitted) ? new Date(item.dateSubmitted).toLocaleDateString() : null },
    { title: 'Sample received on', key: 'dateReceived', sortRaw: sort('dateReceived'), value: (item: Outbreak) => (item && item.dateReceived) ? new Date(item.dateReceived).toLocaleDateString() : null },
  ])
  const status = ref<Map<string, Status>>(outbreakStatus)
  const host = ref<Map<string, Host>>(outbreakHosts)

  // COMPUTED
  const isAdmin: ComputedRef<boolean> = computed(() => {
    if (store.token && store.token.token && store.token.user && store.token.user.isAdmin) {
      return true
    } else {
      return false
    }
  })

  const currentUserId: ComputedRef<number | undefined> = computed(() => {
    if (store.token && store.token.user) {
      return store.token.user.userId
    } else {
      return undefined
    }
  })

  const statusOptions: ComputedRef<SelectOption<string>[]> = computed(() => {
    const result: SelectOption<string>[] = []

    status.value.forEach((value: Status) => {
      result.push({
        title: value.text,
        value: value.dbValue,
      })
    })

    return result
  })

  const varietyOptions: ComputedRef<SelectOption<number>[]> = computed(() => {
    if (varieties.value) {
      return varieties.value.map(s => {
        return {
          value: s.varietyId,
          title: s.varietyName
        }
      })
    } else {
      return []
    }
  })

  const severityOptions: ComputedRef<SelectOption<number>[]> = computed(() => {
    if (severities.value) {
      return severities.value.map(s => {
        return {
          value: s.severityId,
          title: s.severityName
        }
      })
    } else {
      return []
    }
  })

  const sourceOptions: ComputedRef<SelectOption<number>[]> = computed(() => {
    if (sources.value) {
      return sources.value.map(s => {
        return {
          value: s.sourceId,
          title: s.sourceName
        }
      })
    } else {
      return []
    }
  })

  const outbreaksWithGps: ComputedRef<Outbreak[]> = computed(() => {
    if (outbreaks.value) {
      return outbreaks.value.filter(o => o.viewLatitude !== undefined && o.viewLatitude !== null && o.viewLongitude !== undefined && o.viewLongitude !== null)
    } else {
      return []
    }
  })

  // METHODS
  function sort (field: string) {
    return function sort (a: any, b: any) {
      if (a === null) {
          return 1
      }
      if (b === null) {
          return -1
      }
      if (a[field] === b[field]) {
          return 0
      }
      return a[field] < b[field] ? -1 : 1
    }
  }

  function setPostcode () {
    postcodeValid.value = undefined

    if (postcodeTemp.value) {
      axios.get(`https://api.postcodes.io/outcodes/${postcodeTemp.value}`).then(() => {
        postcode.value = postcodeTemp.value
      }).catch(() => {
        postcodeValid.value = false
      })
    } else {
      postcode.value = undefined
    }
  }

  function setOutbreakCode () {
    outbreakCode.value = outbreakCodeTemp.value
  }

  function downloadReport () {
    axiosCall<Blob>({ url: 'outbreaks/csv', dataType: 'blob', params: {
        source: selectedSource.value,
        severity: selectedSeverity.value,
        variety: selectedVariety.value,
        year: selectedYear.value,
        status: selectedStatus.value,
        outbreakCode: outbreakCode.value,
        outcode: postcode.value,
        userId: onlyShowUserData.value ? store.token?.user?.userId : undefined,
      } 
    })
    .then(result => {
      const reader = new FileReader();
      reader.onload = e => {
        // @ts-ignore
        const data = e.target?.result.split(/\r?\n/).map((d: string) => d.split('\t'))

        const ws = XLSX.utils.aoa_to_sheet(data)
        const wb = XLSX.utils.book_new()
        XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')
        XLSX.writeFile(wb, `${new Date().toISOString().split('T')[0]}-fight-against-blight-report.xlsx`)
      }
      reader.readAsText(result)
    })
  }

  function update () {
    page.value = 1

    const params: any = {
      source: selectedSource.value,
      severity: selectedSeverity.value,
      variety: selectedVariety.value,
      year: selectedYear.value,
      status: selectedStatus.value,
      outbreakCode: outbreakCode.value,
      outcode: postcode.value,
      page: page.value || 1,
    }

    emitter.emit('plausible-event', { key: 'outbreak-filtering', props: params })

    if (onlyShowUserData.value && store.token?.user?.userId) {
      params.userId = store.token?.user?.userId
    }

    router.replace({ query: params })

    loading.value = true
    axiosCall<Outbreak[]>({ url: 'outbreaks', params: params })
      .then((result: Outbreak[]) => {
        loading.value = false
        outbreaks.value = result

        if (forcePage.value && forcePage.value !== 1) {
          nextTick(() => {
            page.value = forcePage.value
            forcePage.value = undefined
          })
        }
      })
      .catch(() => {
        loading.value = false
      })
  }

  // WATCH
  // Watch for changes on the filtering options
  watch(selectedSource, async () => update())
  watch(selectedSeverity, async () => update())
  watch(selectedVariety, async () => update())
  watch(selectedYear, async () => update())
  watch(selectedStatus, async () => update())
  watch(outbreakCode, async () => update())
  watch(postcode, async () => update())
  watch(onlyShowUserData, async () => update())

  watch(page, async (newValue) => {
    const q = Object.assign({}, router.currentRoute.value.query || {})
    // @ts-ignore
    q.page = newValue
    router.replace({ query: q })
  })

  // Query the database to get all database values
  axiosCall<Severity[]>({ url: 'severities' })
    .then((result: Severity[]) => {
      severities.value = result
    })
  axiosCall<Variety[]>({ url: 'varieties' })
    .then((result: Variety[]) => {
      varieties.value = result
    })
  axiosCall<Source[]>({ url: 'sources' })
    .then((result: Source[]) => {
      sources.value = result
    })
  axiosCall<number[]>({ url: 'outbreaks/years' })
    .then((result: number[]) => {
      years.value = result

      if (result.length > 0 && !selectedYear.value) {
        const max = Math.max(...result)

        if (max) {
          selectedYear.value = max
        }
      }
    })
  axiosCall<Outbreak[]>({ url: 'outbreaks' })
    .then((result: Outbreak[]) => {
      outbreaks.value = result
    })

  onMounted(() => {
    if (router.currentRoute.value.query) {
      // Read URL parameters to restore page state
      const q = router.currentRoute.value.query
      selectedSource.value = q.source ? +q.source : undefined
      selectedSeverity.value = q.severity ? +q.severity : undefined
      selectedVariety.value = q.variety ? +q.variety : undefined
      selectedYear.value = q.year ? +q.year : undefined
      selectedStatus.value = q.status ? `${q.status}` : undefined
      outbreakCode.value = q.outbreakCode ? `${q.outbreakCode}` : undefined
      outbreakCodeTemp.value = outbreakCode.value
      postcode.value = q.outcode ? `${q.outcode}` : undefined
      postcodeTemp.value = postcode.value
      onlyShowUserData.value = q.userId !== undefined && q.userId !== null
      forcePage.value = q.page ? +q.page : 1
    }
  })
</script>

<style>

</style>
