<template>
  <section>
    <h2>Sponsors</h2>

    <p>We are grateful to the following companies and organisations for sponsorship of Fight Against Blight in {{ new Date().getFullYear() }}.</p>

    <v-row>
      <v-col
        v-for="sponsor in sponsors"
        :key="`sponsor-${sponsor.name}`"
        class="d-flex align-center pa-5 sponsor-img"
        :cols=12
        :lg=3
        :md=4
      >
        <v-img
          :src="`/img/sponsors/${sponsor.logo}`"
        />
      </v-col>
    </v-row>
  </section>
</template>

<script lang="ts" setup>
interface Sponsor {
  logo: string
  name: string
}

const sponsors = ref<Sponsor[]>([
  { logo: 'certis-belchim.png', name: 'Certis Belchim' },
  { logo: 'upl-openag.png', name: 'UPL OpenAg' },
  { logo: 'basf.svg', name: 'BASF' },
  { logo: 'bayer.svg', name: 'Bayer' },
  { logo: 'corteva.svg', name: 'Corteva' },
  { logo: 'syngenta.svg', name: 'Syngenta' },
  { logo: 'albert-bartlett.png', name: 'Albert Bartlett' },
  { logo: 'agrovista.svg', name: 'Agrovista' },
  { logo: 'frontier.svg', name: 'Frontier' },
  { logo: 'hutchinsons.svg', name: 'Hutchinsons' },
  { logo: 'gb-potatoes.png', name: 'GB Potatoes' },
  { logo: 'mccain.svg', name: 'McCain' },
  { logo: 'scottish-agronomy.png', name: 'Scottish Agronomy' },
  { logo: 'procam.svg', name: 'PROCAM' },
  { logo: 'sruc.svg', name: 'SRUC' },
  { logo: 'spo.png', name: 'Seed Potato Organisation' },
  { logo: 'agrico.svg', name: 'AGRICO' },
  { logo: 'branston.svg', name: 'BRANSTON' },
  { logo: 'agrii.svg', name: 'Agrii' },
])
</script>

<style>
.sponsor-img {
  max-height: 175px;
  width: auto;
}
</style>
