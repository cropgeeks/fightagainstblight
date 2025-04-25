<template>
  <section>
    <h2>Sponsors</h2>

    <p>We are grateful to the following companies and organisations for sponsorship of Fight Against Blight in {{ new Date().getFullYear() }}.</p>

    <v-row
      v-for="row in sponsorsPerRow.keys()"
      :key="`row-${row}`"
      :class="`${row !== 0 ? 'border-t' : null}`"
    >
      <v-col
        v-for="sponsor in sponsorsPerRow.get(row)"
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
  name: string,
  row: number
}

const sponsors = ref<Sponsor[]>([
  { logo: 'certis-belchim.png', name: 'Certis Belchim', row: 0 },
  { logo: 'upl-openag.png', name: 'UPL OpenAg', row: 0 },
  { logo: 'basf.svg', name: 'BASF', row: 1 },
  { logo: 'bayer.svg', name: 'Bayer', row: 1 },
  { logo: 'corteva.svg', name: 'Corteva', row: 1 },
  { logo: 'syngenta.svg', name: 'Syngenta', row: 1 },
  { logo: 'albert-bartlett.png', name: 'Albert Bartlett', row: 1 },
  { logo: 'fmc.svg', name: 'FMC', row: 2 },
  { logo: 'agrovista.svg', name: 'Agrovista', row: 2 },
  { logo: 'frontier.svg', name: 'Frontier', row: 2 },
  { logo: 'hutchinsons.svg', name: 'Hutchinsons', row: 2 },
  { logo: 'gb-potatoes.png', name: 'GB Potatoes', row: 2 },
  { logo: 'mccain.svg', name: 'McCain', row: 2 },
  { logo: 'scottish-agronomy.png', name: 'Scottish Agronomy', row: 2 },
  { logo: 'procam.svg', name: 'PROCAM', row: 2 },
  { logo: 'sruc.svg', name: 'SRUC', row: 3 },
  // { logo: 'spo.png', name: 'Seed Potato Organisation' },
  // { logo: 'agrico.svg', name: 'AGRICO' },
  { logo: 'branston.svg', name: 'BRANSTON', row: 3 },
  { logo: 'agrii.svg', name: 'Agrii', row: 3 },
])

const sponsorsPerRow: ComputedRef<Map<number, Sponsor[]>> = computed(() => {
  const result: Map<number, Sponsor[]> = new Map()

  sponsors.value.forEach(s => {
    const arr = result.get(s.row) || []
    arr.push(s)
    result.set(s.row, arr)
  })

  return result
})
</script>

<style>
.sponsor-img {
  max-height: 175px;
  width: auto;
}
</style>
