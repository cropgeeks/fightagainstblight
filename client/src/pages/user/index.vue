<template>
  <BackButton />

  <section v-if="store.token && store.token.token && store.token.user && store.token.user.isAdmin">
    <h1>Users</h1>

    <v-sheet
      :border="true"
      class="mb-5"
      rounded
    >
      <v-data-table
        :headers="headers"
        :items="filteredUsers"
        no-data-text="No users found."
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
                icon="mdi-account-details"
                size="x-small"
                start
              />

              Users
            </v-toolbar-title>

            <v-text-field
              v-model="searchTerm"
              class="search-term"
              clearable
              hide-details
              label="Search for user"
            />

            <v-btn
              class="me-2"
              color="primary"
              prepend-icon="mdi-plus"
              rounded="lg"
              text="Add a user"
              variant="flat"
              @click="add"
            />
          </v-toolbar>
        </template>

        <template #item.isAdmin="{ item }">
          <v-chip
            :prepend-icon="item.isAdmin ? 'mdi-check-circle' : 'mdi-close-circle'"
            :color="item.isAdmin ? 'success' : 'grey'"
            variant="tonal"
          >
            {{ item.isAdmin ? 'Yes' : 'No' }}
          </v-chip>
        </template>

        <template #item.actions="{ item }">
          <div class="d-flex ga-2 justify-end">
            <v-icon
              color="medium-emphasis"
              icon="mdi-pencil"
              size="small"
              @click="edit(item.userId)"
            />
          </div>
        </template>
      </v-data-table>
    </v-sheet>

    <v-dialog v-model="dialog" max-width="500">
      <v-card
        v-if="record"
        :subtitle="`${isEditing ? 'Update details about this' : 'Create a new'} user`"
        :title="`${isEditing ? 'Edit' : 'Add'} user`"
      >
        <template #text>
          <v-row>
            <v-col cols="12">
              <v-text-field
                v-model="record.userName"
                label="User name"
                :error="usernameValid === false"
                :error-messages="usernameValid === false ? ['Missing user name.'] : null"
              />
            </v-col>

            <v-col cols="12">
              <v-text-field
                v-model="record.email"
                label="Email"
                type="email"
                :error="emailValid === false"
                :error-messages="emailValid === false ? ['Email needs to be provided for new users.'] : null"
              />
            </v-col>

            <v-col cols="12">
              <v-checkbox
                v-model="record.isAdmin"
                hide-details
                label="Is admin"
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
  </section>
  <section v-else>
    <router-link to="/login">Please <router-link to="/login">log in</router-link> as an administrator to edit user information.</router-link>
  </section>
</template>

<script lang="ts" setup>
  import { axiosCall } from '@/plugins/api'
  import type { User } from '@/plugins/types/User'
  import { coreStore } from '@/stores/app'
  // @ts-ignore
  import emitter from 'tiny-emitter/instance'

  interface SnackbarConfig {
    text: string,
    color: string
  }

  // COMPOSITION
  const store = coreStore()

  // REFS
  const searchTerm = ref<string>()
  const users = ref<User[]>([] as User[])
  const record = ref<User>()
  const dialog = ref<boolean>(false)
  const isEditing = ref<boolean>(false)
  const usernameValid = ref<boolean | undefined>()
  const emailValid = ref<boolean | undefined>()
  const snackbar = ref<boolean>(false)
  const snackbarConfig = ref<SnackbarConfig>({
    text: '',
    color: 'success',
  })

  // COMPUTED
  const headers: ComputedRef<any[]> = computed(() => {
    const arr: any[] = [
      { title: 'Id', key: 'userId' },
      { title: 'Name', key: 'userName' },
      { title: 'Email', key: 'email' },
      { title: 'Is admin', key: 'isAdmin' },
      { title: 'Actions', key: 'actions', align: 'end', sortable: false }
    ]

    return arr
  })

  const filteredUsers: ComputedRef<User[]> = computed(() => {
    if (searchTerm.value && searchTerm.value.trim().length > 0) {
      const trimmed = searchTerm.value.trim().toLowerCase()
      if (trimmed === 'admin') {
        return users.value.filter(u => u.isAdmin)
      } else {
        return users.value.filter(u => u.userName?.toLowerCase().includes(trimmed) || u.email?.toLocaleLowerCase().includes(trimmed))
      }
    } else {
      return users.value
    }
  })

  // METHODS
  function edit (id?: number) {
    usernameValid.value = undefined
    isEditing.value = true
    const found = users.value.find(s => s.userId === id)
    if (found) {
      record.value = { ...found }
      dialog.value = true
    }
  }
  function add () {
    usernameValid.value = undefined
    isEditing.value = false
    record.value = { isAdmin: false, userName: '', userId: -1 }
    dialog.value = true
  }
  function save () {
    if (record.value) {
      usernameValid.value = !(!record.value.userName || record.value.userName.trim().length < 1)

      if (!usernameValid.value) {
        return
      }

      if (record.value.userId && record.value.userId !== -1) {
        axiosCall({ url: `users/${record.value.userId}`, method: 'PATCH', params: record.value })
          .then(() => {
            update()
            emitter.emit('plausible-event', { key: 'user-edit' })
            snackbarConfig.value.text = 'User successfully updated.'
            snackbar.value = true
          })
          .catch(e => {
            console.error(e)
          })
      } else {
        emailValid.value = !(!record.value.email || record.value.email.trim().length < 1)

        if (!emailValid.value) {
          return
        }

        axiosCall({ url: 'users', method: 'POST', params: { userName: record.value.userName, isAdmin: record.value.isAdmin, email: record.value.email } })
          .then(() => {
            update()
            emitter.emit('plausible-event', { key: 'user-add' })
            snackbarConfig.value.text = 'User successfully added.'
            snackbar.value = true
          })
          .catch(e => {
            console.error(e)
          })
      }

      dialog.value = false
    }
    
  }

  function update () {
    axiosCall<User[]>({ url: 'users' })
      .then(result => {
        users.value = result
      })
  }

  if (store.token && store.token.token && store.token.user && store.token.user.isAdmin) {
    update()
  }
</script>

<style scoped>
.search-term {
  flex: unset;
  width: max(300px, 25%);
  margin-left: auto;
  margin-right: auto;
}
.flex-unset {
  flex: unset;
}
</style>
