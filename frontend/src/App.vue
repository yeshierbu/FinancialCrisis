<template>
  <div class="min-h-screen bg-gray-50">
    <Navbar
      :current-view="currentView"
      :is-logged-in="isLoggedIn"
      @navigate="handleNavigate"
      @login="handleLogin"
    />

    <main class="mx-auto w-full max-w-7xl px-4 py-8">
      <HomePage
        v-if="currentView === 'home'"
        @start-application="handleNavigate('apply')"
        @view-tutorial="handleNavigate('tutorial')"
        @view-list="handleNavigate('list')"
      />

      <ApplyPage
        v-else-if="currentView === 'apply'"
        @back="handleNavigate('home')"
        @submitted="handleSubmitted"
      />

      <StatusPage
        v-else-if="currentView === 'status'"
        :key="selectedApplicationId || 'latest-status'"
        :application-id="selectedApplicationId"
        @back="handleNavigate('home')"
        @view-list="handleNavigate('list')"
      />

      <ListPage
        v-else-if="currentView === 'list'"
        @back="handleNavigate('home')"
        @view-detail="handleViewDetail"
      />

      <TutorialPage
        v-else-if="currentView === 'tutorial'"
        @back="handleNavigate('home')"
        @start-application="handleNavigate('apply')"
      />

      <LoginPage
        v-else-if="currentView === 'login'"
        @back="handleNavigate('home')"
        @logged-in="handleLoggedIn"
      />

      <ProfilePage
        v-else-if="currentView === 'profile'"
        @back="handleNavigate('home')"
        @logout="handleLogout"
        @navigate="handleNavigate"
      />
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Navbar from './components/Navbar.vue'
import Footer from './components/Footer.vue'
import HomePage from './components/HomePage.vue'
import ApplyPage from './components/ApplyPage.vue'
import StatusPage from './components/StatusPage.vue'
import ListPage from './components/ListPage.vue'
import TutorialPage from './components/TutorialPage.vue'
import LoginPage from './components/LoginPage.vue'
import ProfilePage from './components/ProfilePage.vue'

const currentView = ref('home')
const isLoggedIn = ref(false)
const selectedApplicationId = ref(null)

function handleNavigate(view) {
  currentView.value = view
}

function handleLogin() {
  currentView.value = 'login'
}

function handleLoggedIn() {
  isLoggedIn.value = true
  currentView.value = 'profile'
}

function handleLogout() {
  isLoggedIn.value = false
  currentView.value = 'home'
}

function handleSubmitted(applicationId) {
  selectedApplicationId.value = applicationId
  localStorage.setItem('latestApplicationId', String(applicationId))
  currentView.value = 'status'
}

function handleViewDetail(applicationId) {
  selectedApplicationId.value = applicationId
  localStorage.setItem('latestApplicationId', String(applicationId))
  currentView.value = 'status'
}
</script>
