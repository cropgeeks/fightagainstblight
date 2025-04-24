<template>
  <h1 class="mb-4">Fight against Blight</h1>

  <v-row>
    <v-col :cols=12 :md=8>
      <p class="mb-2">As a part of the research into blight populations, the Potatoes Fight against Blight service has been reporting blight alerts since 2017.</p>
      <p class="mb-2">Fill out the fields below to see a report summary of areas affected in the UK. Changing the date range will allow you to see when blight was reported across the season.</p>
      <p class="mb-2">To report a new outbreak, please use the form on <router-link to="/submit">this page</router-link>.</p>
    </v-col>
    <v-col :cols=12 :md=4 class="d-flex align-center pa-5 sponsor-img">
      <v-img position="right center" src="@/assets/hutton.svg" />
    </v-col>
  </v-row>

  <v-row class="mb-4">
    <v-col
      v-for="banner in banners"
      :key="`banners-${banner.title}`"
      cols="12"
      md="4"
      sm="6"
    >
      <v-card
        link
        :to="banner.to"
      >
        <v-img
          :src="`/img/banners/${banner.img}`"
          class="align-end h-100"
          gradient="to bottom, rgba(0,0,0,.1), rgba(0,0,0,.5)"
          height="200px"
          cover
        >
          <v-card-title class="text-white">{{ banner.title }}</v-card-title>
        </v-img>

        <v-card-text :class="`bg-${banner.color} pt-4 d-flex align-center`">
          <v-icon
            class="me-3"
            :icon="banner.icon"
            size="x-large"
          />
          <span>{{ banner.text }}</span>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>

  <Sponsors />
</template>

<script lang="ts" setup>
  interface Banner {
    title: string
    icon: string
    to: string
    color: string
    text: string
    img: string
  }

  const banners = ref<Banner[]>([
    { title: 'View reported outbreaks', img: 'outbreak-list.jpg', color: 'blue-darken-1', icon: 'mdi-table-eye', to: '/outbreak', text: 'Submit a new potential outbreak to the system. The samples will be checked and the outbreak will be confirmed or marked as negative.' },
    { title: 'Report a new outbreak', img: 'report-outbreak.jpg', color: 'cyan-darken-1', icon: 'mdi-file-document-alert-outline', to: '/submit', text: 'Submit a new potential outbreak to the system. The samples will be checked and the outbreak will be confirmed or marked as negative.' },
    { title: 'Scout sign in', img: 'login.jpg', color: 'green-darken-1', icon: 'mdi-login', to: '/login', text: 'Sign in with your email address to get access to your submitted outbreaks and to report a new outbreak.' },
  ])

</script>