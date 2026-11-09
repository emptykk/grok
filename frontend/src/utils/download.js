import http from '@/utils/request'

export async function downloadFile(url, filename, params) {
  const blob = await http.get(url, { responseType: 'blob', params })
  const href = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = href
  a.download = filename
  a.click()
  URL.revokeObjectURL(href)
}
