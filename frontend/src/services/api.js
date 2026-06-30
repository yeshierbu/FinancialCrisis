const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export class ApiError extends Error {
  constructor(message, details) {
    super(message)
    this.name = 'ApiError'
    this.details = details
  }
}

async function request(path, options = {}) {
  const headers = {
    Accept: 'application/json',
    ...(options.headers || {}),
  }

  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
    })
  } catch (error) {
    throw new ApiError('无法连接后端服务，请确认 Spring Boot 已在 8080 端口启动。', error)
  }

  let payload = null
  try {
    payload = await response.json()
  } catch {
    payload = null
  }

  if (!response.ok) {
    throw new ApiError(payload?.message || `请求失败：${response.status}`, payload)
  }

  if (payload && typeof payload.code !== 'undefined' && payload.code !== 0) {
    throw new ApiError(payload.message || '业务处理失败', payload)
  }

  return payload?.data ?? payload
}

export const loanApi = {
  listApplications() {
    return request('/api/loan/applications')
  },

  createApplication(data) {
    return request('/api/loan/applications', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  getApplication(applicationId) {
    return request(`/api/loan/applications/${applicationId}`)
  },

  getApplicationStatus(applicationId) {
    return request(`/api/loan/applications/${applicationId}/status`)
  },

  uploadDocument(applicationId, data) {
    return request(`/api/loan/applications/${applicationId}/documents`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  getReport(applicationId) {
    return request(`/api/loan/applications/${applicationId}/report`)
  },
}
