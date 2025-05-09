<template>
  <BackButton
    text="Back to outbreaks"
    to="/outbreak"
  />

  <div v-if="outbreak">
    <h1>Outbreak details: {{ outbreak.outbreakCode }}</h1>

    <div
      v-if="isAdmin"
      class="my-5"
    >
      <v-btn
        class="me-2"
        color="primary"
        :disabled="loading"
        prepend-icon="mdi-send"
        @click="notifyOwner"
      >
        Notify owner
      </v-btn>

      <v-btn
        class="me-2"
        color="error"
        :disabled="loading"
        prepend-icon="mdi-delete"
        text="Delete outbreak"
        @click="deleteOutbreak"
      />
    </div>

    <v-sheet
      :border="true"
      class="mb-5"
      rounded
    >
      <v-toolbar flat>
        <v-toolbar-title>
          <v-icon
            color="medium-emphasis"
            icon="mdi-map-marker-alert"
            size="x-small"
            start
          />

          Outbreak
        </v-toolbar-title>
      </v-toolbar>
      <v-row class="my-2 mx-1">
        <v-col
          :cols="12"
          :lg="3"
          :md="4"
          :sm="6"
        >
          <v-text-field
            v-model="outbreak.dateSubmitted"
            readonly
            persistent-placeholder
            label="Submitted on"
            type="date"
          />
        </v-col>
        <v-col
          :cols="12"
          :lg="3"
          :md="4"
          :sm="6"
        >
          <v-text-field
            v-model="outbreak.dateReceived"
            :clearable="isAdmin"
            :readonly="!isAdmin"
            persistent-placeholder
            label="Received on"
            type="date"
          />
        </v-col>
        <v-col
          :cols="12"
          :lg="3"
          :md="4"
          :sm="6"
        >
          <v-autocomplete
            v-if="isAdmin"
            v-model="outbreak.userId"
            label="Owner"
            :items="userOptions"
          />
          <v-text-field
            v-else-if="isOwner"
            :placeholder="store.token?.user?.userName"
            persistent-placeholder
            readonly
            label="Owner"
          />
        </v-col>
        <v-col
          :cols="12"
          :lg="3"
          :md="4"
          :sm="6"
        >
          <v-autocomplete
            v-model="outbreak.status"
            :readonly="!isAdmin"
            label="Status"
            :items="statusOptions"
          />
        </v-col>
        <v-col
          v-if="isOwner || isAdmin"
          :cols="12"
          :lg="3"
          :md="4"
          :sm="6"
        >
          <v-textarea
            v-model="outbreak.userComments"
            readonly
            label="User comments"
          />
        </v-col>
        <v-col
          v-if="isAdmin"
          :cols="12"
          :lg="3"
          :md="4"
          :sm="6"
        >
          <v-textarea
            v-model="outbreak.adminComments"
            label="Admin comments"
          />
        </v-col>
      </v-row>

      <v-btn
        v-if="isAdmin"
        class="mb-5 mx-4"
        color="primary"
        :disabled="loading"
        prepend-icon="mdi-content-save"
        @click="submitOutbreak"
      >
        Update outbreak
      </v-btn>
    </v-sheet>

    <v-sheet
      :border="true"
      class="mb-5"
      rounded
    >
      <v-data-table
        :headers="headers"
        :items="subsamples"
        :items-per-page="-1"
        :no-data-text="(!isAdmin && !isOwner) ? 'Sample information is private to the scout who reported it.' : undefined"
        :sort-by="[{ key: 'subsampleCode', order: 'asc' }]"
      >
        <template #top>
          <v-toolbar
            class="justify-space-between"
            flat
          >
            <v-toolbar-title
              class="flex-unset"
            >
              <v-icon
                color="medium-emphasis"
                icon="mdi-archive"
                size="x-small"
                start
              />

              Subsamples
            </v-toolbar-title>

            <v-autocomplete
              v-if="isAdmin"
              v-model="allSubsampleVariety"
              class="variety-dropdown"
              :readonly="!isAdmin"
              hide-details
              label="Assign variety to all subsamples"
              :items="varietyOptions"
            >
              <template #append>
                <v-icon
                  v-if="isAdmin && atLeastOneSubsampleVariety"
                  color="error"
                  icon="mdi-delete"
                  @click="clearSubsampleVariety"
                />
              </template>
            </v-autocomplete>

            <v-btn
              v-if="isAdmin"
              class="me-2"
              color="primary"
              prepend-icon="mdi-plus"
              rounded="lg"
              text="Add a subsample"
              variant="flat"
              @click="add"
            />
          </v-toolbar>
        </template>

        <template #item.myceliaPellet="{ item }">
          <v-chip
            :prepend-icon="item.myceliaPellet ? 'mdi-check-circle' : 'mdi-close-circle'"
            :color="item.myceliaPellet ? 'success' : 'grey'"
            variant="tonal"
          >
            {{ item.myceliaPellet ? 'Yes' : 'No' }}
          </v-chip>
        </template>

        <template #item.cultureSlope="{ item }">
          <v-chip
            :prepend-icon="item.cultureSlope ? 'mdi-check-circle' : 'mdi-close-circle'"
            :color="item.cultureSlope ? 'success' : 'grey'"
            variant="tonal"
          >
            {{ item.cultureSlope ? 'Yes' : 'No' }}
          </v-chip>
        </template>

        <template #footer.prepend>
          <div v-if="isAdmin" class="me-auto">
            <v-btn
              class=" ms-2"
              color="primary"
              :disabled="loading"
              prepend-icon="mdi-content-save"
              @click="submitSubsamples"
            >
              Update subsamples
            </v-btn>
          </div>
        </template>

        <template #item.subsampleId="{ item }">
          <v-chip
            :color="item.subsampleId ? 'success' : 'info'"
            :text="item.subsampleId ? 'Existing' : 'Pre-created'"
          />
        </template>

        <template #item.adminComments="{ item }">
          <div class="admin-comments" :title="item.adminComments">{{ item.adminComments }}</div>
        </template>

        <template #item.actions="{ item }">
          <div v-if="isAdmin" class="d-flex ga-2 justify-end">
            <v-icon
              color="medium-emphasis"
              icon="mdi-pencil"
              size="small"
              @click="edit(item.tempId)"
            />
            <v-icon
              color="medium-emphasis"
              icon="mdi-delete"
              size="small"
              @click="deleteSubsample(item.tempId)"
            />
          </div>
        </template>
      </v-data-table>
    </v-sheet>

    <OutbreakMap
      v-if="hasGps"
      :outbreaks="[outbreak]"
      :show-outbreak-link="false"
    />

    <v-dialog v-model="dialog" max-width="500">
      <v-card
        v-if="record && isAdmin"
        :subtitle="`${isEditing ? 'Update details about this' : 'Create a new'} subsample`"
        :title="`${isEditing ? 'Edit' : 'Add'} subsample`"
      >
        <template #text>
          <v-row>
            <v-col cols="12">
              <v-text-field
                v-model="record.subsampleCode"
                :error="subsampleCodeValid === false"
                :error-messages="subsampleCodeValid === false ? ['Duplicate subsample code.'] : null"
                label="Subsample code"
              />
            </v-col>

            <v-col cols="12" md="6">
              <v-autocomplete
                v-model="record.varietyId"
                clearable
                hide-details
                label="Variety"
                :items="varietyOptions"
              />
            </v-col>

            <v-col cols="12" md="6">
              <v-autocomplete
                v-model="record.genotypeId"
                clearable
                hide-details
                label="Genotype"
                :items="genotypeOptions"
              />
            </v-col>

            <v-col cols="12" md="6">
              <v-autocomplete
                v-model="record.material"
                hide-details
                :items="sampleMaterials"
                label="Material"
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field
                v-model="record.dateGenotyped"
                clearable
                hide-details
                label="Genotyped on"
                type="date"
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-autocomplete
                v-model="record.matingType"
                hide-details
                :items="matingTypeOptions"
                label="Mating type"
              />
              <v-checkbox
                v-model="record.myceliaPellet"
                hide-details
                label="Mycelia pellet"
              />
              <v-checkbox
                v-model="record.cultureSlope"
                hide-details
                label="Culture slope"
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-textarea
                v-model="record.adminComments"
                label="Admin comments"
              />
            </v-col>
          </v-row>
        </template>

        <v-divider />

        <v-card-actions class="bg-surface-light">
          <v-btn
            text="Cancel"
            variant="plain"
            @click="dialog = false"
          />

          <v-spacer />

          <v-btn
            text="Confirm"
            @click="save"
          />
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar
      v-model="snackbar"
      :color="snackbarConfig.color"
      multi-line
    >
      {{ snackbarConfig.text }}
    </v-snackbar>

    <ConfirmModal ref="confirmModal" />
  </div>
  <h3 v-else>No data found</h3>
</template>

<script lang="ts" setup>
  import OutbreakMap from '@/components/OutbreakMap.vue'

  import { axiosCall } from '@/plugins/api'
  import type { Subsample } from '@/plugins/types/Subsample'
  import type { Outbreak } from '@/plugins/types/Outbreak'
  import { coreStore } from '@/stores/app'
  import { getId } from '@/plugins/id'
  import { isValidLatLng } from '@/plugins/misc'
  import type { Variety } from '@/plugins/types/Variety'
  import type { SelectOption } from '@/plugins/types/SelectOption'
  import type { User } from '@/plugins/types/User'
  import { outbreakStatus, type Status } from '@/plugins/constants'
  import type { Genotype } from '@/plugins/types/Genotype'

  const store = coreStore()
  const route = useRoute('/outbreak/[id]')
  const router = useRouter()

  interface SnackbarConfig {
    text: string,
    color: string
  }

  const confirmModal = ref()
  const outbreakId = ref<number>(+(route.params.id || -1))
  const outbreak = ref<Outbreak>()
  const allSubsampleVariety = ref<number>()
  const varieties = ref<Variety[]>([])
  const genotypes = ref<Genotype[]>([])
  const sampleMaterials = ref<string[]>([])
  const users = ref<User[]>([])
  const loading = ref<boolean>(false)
  // const selectedVariety = ref<number>()
  const subsamples = ref<Subsample[]>([])
  const subsampleCodeValid = ref<boolean | undefined>()
  const subsampleCodesInUse = ref<string[]>([])
  const status = ref<Map<string, Status>>(outbreakStatus)
  const dialog = ref<boolean>(false)
  const isEditing = ref<boolean>(false)
  const record = ref<Subsample>()
  const snackbar = ref<boolean>(false)
  const snackbarConfig = ref<SnackbarConfig>({
    text: '',
    color: 'success',
  })

  const headers: ComputedRef<any[]> = computed(() => {
    const arr: any[] = [
      { title: 'Pre-created/existing', key: 'subsampleId' },
      { title: 'Outbreak code', key: 'outbreakCode' },
      { title: 'Subsample code', key: 'subsampleCode' },
      { title: 'Variety', key: 'varietyName' },
      { title: 'Material', key: 'material' },
      { title: 'Genotyped on', key: 'dateGenotyped', value: (item: Subsample) => (item && item.dateGenotyped) ? new Date(item.dateGenotyped).toLocaleDateString() : null },
      { title: 'Genotype', key: 'genotypeName' }
    ]

    if (isAdmin.value) {
      arr.push(
        { title: 'Mycelia pellet', key: 'myceliaPellet' },
        { title: 'Culture slope', key: 'cultureSlope' },
        { title: 'Mating type', key: 'matingType' },
        { title: 'Admin comment', key: 'adminComments' },
        { title: 'Actions', key: 'actions', align: 'end', sortable: false }
      )
    }

    return arr
  })

  const hasGps: ComputedRef<boolean> = computed(() => {
    if (outbreak.value) {
      if (isOwner.value || isAdmin.value) {
        return isValidLatLng(outbreak.value.realLatitude, outbreak.value.realLongitude)
      } else {
        return isValidLatLng(outbreak.value.viewLatitude, outbreak.value.viewLongitude)
      }
    }

    return false
  })

  const isOwner: ComputedRef<boolean> = computed(() => {
    if (outbreak.value && store.token && store.token.token && store.token.user && store.token.user.userId === outbreak.value.userId) {
      return true
    } else {
      return false
    }
  })

  const isAdmin: ComputedRef<boolean> = computed(() => {
    if (outbreak.value && store.token && store.token.token && store.token.user && store.token.user.isAdmin) {
      return true
    } else {
      return false
    }
  })

  const atLeastOneSubsampleVariety: ComputedRef<boolean> = computed(() => {
    if (outbreak.value && subsamples.value) {
      return subsamples.value.some(s => s.varietyId)
    } else {
      return false
    }
  })

  const matingTypeOptions: ComputedRef<SelectOption<string>[]> = computed(() => {
    const result: SelectOption<string>[] = [
      { title: 'A1', value: 'A1' },
      { title: 'A2', value: 'A2' },
    ]

    return result
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
  const genotypeOptions: ComputedRef<SelectOption<number>[]> = computed(() => {
    if (genotypes.value) {
      return genotypes.value.map(g => {
        return {
          value: g.genotypeId,
          title: g.genotypeName
        }
      })
    } else {
      return []
    }
  })
  const userOptions: ComputedRef<SelectOption<number>[]> = computed(() => {
    if (users.value) {
      return users.value.sort((a: User, b: User) => a.userName.localeCompare(b.userName)).map(s => {
        return {
          value: s.userId,
          title: s.userName
        }
      })
    } else {
      return []
    }
  })

  axiosCall<Variety[]>({ url: 'varieties' })
    .then(result => {
      varieties.value = result
    })
  axiosCall<Genotype[]>({ url: 'ssr_genotypes' })
    .then(result => {
      genotypes.value = result
    })
  axiosCall<User[]>({ url: 'users' })
    .then(result => {
      users.value = result
    })
  axiosCall<string[]>({ url: 'subsamples/materials' })
    .then(result => {
      if (result) {
        result.sort((a, b) => a.localeCompare(b))
        sampleMaterials.value = result
      } else {
        sampleMaterials.value = []
      }
    })

  watchEffect(async () => {
    if (outbreak.value && outbreak.value.userId && users.value) {
      const user = users.value.find(u => u.userId === outbreak.value?.userId)
      if (user) {
        outbreak.value.userName = user.userName
        outbreak.value.userId = user.userId
        outbreak.value.userEmail = user.email
      }
    }
  })

  watch(allSubsampleVariety, async newValue => {
    if (newValue) {
      const variety = varieties.value.find(v => v.varietyId === newValue)
      if (variety) {
        subsamples.value.forEach(s => {
          s.varietyId = variety.varietyId
          s.varietyName = variety.varietyName
        })
      }

      allSubsampleVariety.value = undefined
    }
  })

  function clearSubsampleVariety () {
    confirmModal.value.open(undefined, 'Remove subsample variety?')
      .then((response: boolean) => {
        if (response) {
          subsamples.value.forEach(s => {
            delete s.varietyId
            delete s.varietyName
          })
        }
      })
  }

  function update (updateSubsamples: boolean) {
    axiosCall<Outbreak>({ url: `outbreaks/${outbreakId.value}` })
      .then((o: Outbreak) => {
        outbreak.value = o

        if (updateSubsamples && o) {
          axiosCall<Subsample[]>({ url: `outbreaks/${outbreakId.value}/subsamples` })
            .then((s: Subsample[]) => {
              let temp: Subsample[] = []

              if (s.length < 1) {
                temp = [
                  { varietyId: o.reportedVarietyId, material: 'Leaf', subsampleCode: 'A', myceliaPellet: false, cultureSlope: false, matingType: undefined },
                  { varietyId: o.reportedVarietyId, material: 'Leaf', subsampleCode: 'B', myceliaPellet: false, cultureSlope: false, matingType: undefined },
                  { varietyId: o.reportedVarietyId, material: 'Leaf', subsampleCode: 'C', myceliaPellet: false, cultureSlope: false, matingType: undefined },
                  { varietyId: o.reportedVarietyId, material: 'Leaf', subsampleCode: 'D', myceliaPellet: false, cultureSlope: false, matingType: undefined },
                  { varietyId: o.reportedVarietyId, material: 'FTA', subsampleCode: 'E', myceliaPellet: false, cultureSlope: false, matingType: undefined },
                  { varietyId: o.reportedVarietyId, material: 'FTA', subsampleCode: 'F', myceliaPellet: false, cultureSlope: false, matingType: undefined },
                  { varietyId: o.reportedVarietyId, material: 'FTA', subsampleCode: 'G', myceliaPellet: false, cultureSlope: false, matingType: undefined },
                  { varietyId: o.reportedVarietyId, material: 'FTA', subsampleCode: 'H', myceliaPellet: false, cultureSlope: false, matingType: undefined },
                ]

                if (varieties.value) {
                  temp.forEach(s => {
                    if (s.varietyId) {
                      s.varietyName = varieties.value.find(v => v.varietyId === s.varietyId)?.varietyName
                    }
                  })
                }
              } else {
                temp = s
              }

              temp.forEach(s => {
                s.tempId = getId()
                s.outbreakCode = o.outbreakCode
                s.outbreakId = o.outbreakId
                // if (varieties.value && s.varietyId) {
                //   s.varietyName = varieties.value.find(v => v.varietyId === s.varietyId)?.varietyName
                // }
              })

              subsamples.value = temp
            })
        }
      })
  }
  function deleteSubsample (tempId: string | undefined) {
    confirmModal.value.open(undefined, 'Delete selected subsample?')
      .then((response: boolean) => {
        if (response) {
          subsamples.value = subsamples.value.filter(s => s.tempId !== tempId)
        }
      })
  }
  function setSubsampleCodesInUse () {
    if (subsamples.value) {
      const set = new Set<string>()

      subsamples.value.filter(s => record.value ? (record.value.subsampleCode !== s.subsampleCode) : true).forEach(s => {
        if (s.subsampleCode) {
          set.add(s.subsampleCode.toLocaleLowerCase())
        }
      })

      subsampleCodesInUse.value = [...set]
    } else {
      subsampleCodesInUse.value = []
    }
  }
  function deleteOutbreak () {
    confirmModal.value.open(undefined, 'Deleted outbreaks cannot be restored!<br/>Are you sure you want to delete this outbreak?', { color: 'error' })
      .then((response: boolean) => {
        if (response) {
          loading.value = true
          axiosCall({ url: `outbreaks/${outbreak.value?.outbreakId}`, method: 'DELETE' })
            .then(() => {
              loading.value = false
              router.push('/')
            })
            .catch(e => {
              loading.value = false
              console.error(e)
            })
        }
      })
  }
  function edit (tempId: string | undefined) {
    subsampleCodeValid.value = undefined
    isEditing.value = true
    const found = subsamples.value.find(s => s.tempId === tempId)
    record.value = { ...found }
    dialog.value = true
    setSubsampleCodesInUse()
  }
  function add () {
    subsampleCodeValid.value = undefined
    isEditing.value = false
    record.value = { tempId: getId(), outbreakId: outbreak.value?.outbreakId, outbreakCode: outbreak.value?.outbreakCode }
    dialog.value = true
    setSubsampleCodesInUse()
  }
  function save () {
    if (record.value) {
      const code = record.value.subsampleCode

      if (code && subsampleCodesInUse.value.includes(code.toLocaleLowerCase())) {
        subsampleCodeValid.value = false
        return
      }

      if (record.value.varietyId) {
        const variety = varieties.value.find(v => v.varietyId === record.value?.varietyId)

        if (variety) {
          record.value.varietyName = variety.varietyName
        } else {
          record.value.varietyName = undefined
        }
      } else {
        record.value.varietyName = undefined
      }

      if (isEditing.value) {
        if (record.value.genotypeId) {
          const genotype = genotypes.value.find(g => g.genotypeId === record.value?.genotypeId)

          if (genotype) {
            record.value.genotypeName = genotype.genotypeName
          } else {
            record.value.genotypeName = undefined
          }
        } else {
          record.value.genotypeName = undefined
        }

        const index = subsamples.value.findIndex(s => s.tempId === record.value?.tempId)
        subsamples.value[index] = { ...record.value }
      } else {
        subsamples.value.push(record.value)
      }
      dialog.value = false
    }
  }
  function submitOutbreak () {
    loading.value = true
    axiosCall({ url: `outbreaks/${outbreak.value?.outbreakId}`, method: 'PATCH', params: outbreak.value })
      .then(() => {
        loading.value = false
        update(false)
        snackbarConfig.value.text = 'Outbreak successfully updated.'
        snackbar.value = true
      })
      .catch(e => {
        loading.value = false
        console.error(e)
      })
  }
  function notifyOwner () {
    loading.value = true
    axiosCall({ url: `outbreaks/${outbreak.value?.outbreakId}/notify` })
      .then(() => {
        loading.value = false
        snackbarConfig.value.text = 'Outbreak owner successfully notified.'
        snackbar.value = true
      })
      .catch(e => {
        console.error(e)
        loading.value = false
      })
  }
  function submitSubsamples () {
    loading.value = true
    axiosCall({ url: `outbreaks/${outbreak.value?.outbreakId}/subsamples`, method: 'POST', params: subsamples.value })
      .then(() => {
        loading.value = false
        update(true)
      })
      .catch(e => {
        loading.value = false
        console.error(e)
      })
  }

  update(true)
</script>

<style scoped>
.variety-dropdown {
  flex: unset;
  width: max(300px, 25%);
  margin-left: auto;
  margin-right: auto;
}
.flex-unset {
  flex: unset;
}
.admin-comments {
  max-width: 150px;
  overflow-x: hidden;
  text-overflow: ellipsis;
}
</style>
