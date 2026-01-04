<script setup lang="ts">
import { computed } from 'vue'
import { statusColorMap, statusLabelMap } from '../constants/recording'
import type { RecordingStatusType } from '../types/recording'

interface Props {
  status: RecordingStatusType
}

const props = defineProps<Props>()

const colorClass = computed(() => `status-badge--${statusColorMap[props.status]}`)
const label = computed(() => statusLabelMap[props.status])
</script>

<template>
  <span class="status-badge" :class="colorClass">{{ label }}</span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #1f1f1f;
}

.status-badge--gray {
  background: #e5e5e5;
}

.status-badge--green {
  background: #cfe9cf;
}

.status-badge--yellow {
  background: #fff2b2;
}

.status-badge--blue {
  background: #cfe3ff;
}

.status-badge--red {
  background: #ffd6d6;
}
</style>
