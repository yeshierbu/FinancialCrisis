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

  uploadDocument(applicationId, documentType, file) {
    const formData = new FormData()
    formData.append('documentType', documentType)
    formData.append('file', file)
    return request(`/api/loan/applications/${applicationId}/documents`, {
      method: 'POST',
      body: formData,
    })
  },

  getReport(applicationId) {
    return request(`/api/loan/applications/${applicationId}/report`)
  },
}

function toQueryString(params) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value))
    }
  })
  const value = query.toString()
  return value ? `?${value}` : ''
}

export const adminApi = {
  listPolicyDocuments() {
    return request('/api/admin/policy-knowledge/documents')
  },

  uploadPolicyDocument(data, file) {
    const formData = new FormData()
    Object.entries(data).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') formData.append(key, value)
    })
    formData.append('file', file)
    return request('/api/admin/policy-knowledge/documents', {
      method: 'POST',
      body: formData,
    })
  },

  listPendingReviews(params = {}) {
    return request(`/api/admin/reviews/pending${toQueryString(params)}`)
  },

  getReviewDetail(applicationId) {
    return request(`/api/admin/reviews/${applicationId}`)
  },

  approveReview(applicationId, data) {
    return request(`/api/admin/reviews/${applicationId}/approve`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  rejectReview(applicationId, data) {
    return request(`/api/admin/reviews/${applicationId}/reject`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

}
