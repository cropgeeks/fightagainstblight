<template>
  <h1 class="mb-4">Fight Against Blight</h1>

  <v-row>
    <!-- Stuff along the top -->
    <v-col :cols=12 :md=8>
      <p class="mb-2">Welcome to the revised Fight Against Blight website.  Thanks to the below sponsors and the network of blight scouts this reporting service and Hutton's research on the potato late blight pathogen population has been supporting the integrated management of potato late blight since 2006.</p>
      <v-list>
        <v-list-item prepend-icon="mdi-file-document-alert-outline">To report a new outbreak, please use the form on <router-link to="/submit">this page</router-link>.</v-list-item>
        <v-list-item prepend-icon="mdi-file-sign">To register as a new FAB scout, please complete this <a href="https://forms.office.com/e/DRJYgDzNHd">registration form</a> and we will be in touch.</v-list-item>
        <v-list-item prepend-icon="mdi-email-outline">If you would like more information about Fight Against Blight or encounter any issues, please contact us at <a href="mailto:fab@hutton.ac.uk">fab@hutton.ac.uk</a>.</v-list-item>
      </v-list>
    </v-col>
    <v-col :cols=12 :md=4 class="d-flex align-center pa-5 sponsor-img">
      <!-- Hutton logos, hidden or right aligned depending on screen size -->
      <v-img class="d-none d-md-inline-block" position="right center" src="@/assets/hutton.svg" />
      <v-img class="d-md-none" position="center center" src="@/assets/hutton.svg" />
    </v-col>
  </v-row>

  <v-row class="mb-4">
    <!-- The cards for the banners -->
    <v-col
      v-for="banner in banners"
      :key="`banners-${banner.id}`"
      cols="12"
      md="3"
      sm="6"
    >
      <v-card
        link
        :to="banner.to"
        :href="banner.href"
      >
        <v-img
          :src="`/img/banners/${banner.img}`"
          class="align-end h-100"
          gradient="to bottom, rgba(0,0,0,.1), rgba(0,0,0,.5)"
          height="200px"
          cover
        >
          <v-card-title class="text-white">
            {{ banner.title }}
          </v-card-title>
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

      <!-- Place the corresponding buttons below certain cards -->
      <v-btn
        v-if="banner.id === 'report-outbreak'"
        block
        class="mt-3"
        href="/data/2025-scout-response-form.docx"
        prepend-icon="mdi-file-word"
        text="Scout Response Form"
      />
      <v-btn
        v-if="banner.id === 'sign-in'"
        block
        class="mt-3"
        href="https://forms.office.com/e/DRJYgDzNHd"
        prepend-icon="mdi-file-sign"
        text="Register as a scout"
      />
    </v-col>
  </v-row>

  <!-- Sponsors along the bottom -->
  <Sponsors />
</template>

<script lang="ts" setup>
  // Template interface for a banner
  interface Banner {
    id: string
    title: string
    icon: string
    to?: string
    href?: string
    color: string
    text: string
    img: string
  }

  const banners = ref<Banner[]>([
    { id: 'view-outbreaks', title: 'View reported outbreaks', img: 'outbreak-list.jpg', color: 'blue-darken-1', icon: 'mdi-table-eye', to: '/outbreak', text: 'View all reported outbreaks and their confirmation status as a list or on a map. Search and filter the results to find what you\'re looking for.' },
    { id: 'report-outbreak', title: 'Report a new outbreak', img: 'report-outbreak.jpg', color: 'cyan-darken-1', icon: 'mdi-file-document-alert-outline', to: '/submit', text: 'Submit a new potential outbreak to the system. The samples will be checked and the outbreak will be confirmed or marked as negative.' },
    { id: 'sign-in', title: 'Scout sign in', img: 'login.jpg', color: 'green-darken-1', icon: 'mdi-login', to: '/login', text: 'Click here to sign in with your registered email address to get access to your submitted outbreaks and to report a new outbreak.' },
    { id: 'blight-spy', title: 'BlightSpy forecast', img: 'blightspy.jpg', color: 'light-green-darken-1', icon: 'mdi-weather-partly-rainy', href: 'https://blightspy.huttonltd.com/', text: 'BlightSpy allows potato growers and agronomists to monitor weather forecasts for the predicted occurrence of Hutton Criteria blight risk.' },
  ])

</script>

<style scoped>
:deep(.v-btn__content) {
    white-space: pre-wrap;
}
</style>
