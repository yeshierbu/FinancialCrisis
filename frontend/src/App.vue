<template>
  <div class="min-h-screen">
    <Navbar
      v-if="isLoggedIn"
      :current-view="currentView"
      :role="session.role"
      :current-user="session"
      @navigate="handleNavigate"
      @logout="handleLogout"
    />

    <main
      :class="[
        'mx-auto w-full px-4 sm:px-6',
        isLoggedIn ? 'max-w-7xl py-8 lg:py-10' : 'max-w-none p-0',
      ]"
    >
      <LoginPage
        v-if="currentView === 'login'"
        @logged-in="handleLoggedIn"
      />

      <AdminDashboard
        v-else-if="currentView === 'admin'"
        @view-list="handleNavigate('list')"
      />

      <HomePage
        v-else-if="currentView === 'home'"
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
        @back="handleNavigate(defaultView)"
        @view-list="handleNavigate('list')"
      />

      <ListPage
        v-else-if="currentView === 'list'"
        :admin-mode="session.role === 'admin'"
        @back="handleNavigate(defaultView)"
        @view-detail="handleViewDetail"
      />

      <TutorialPage
        v-else-if="currentView === 'tutorial'"
        @back="handleNavigate('home')"
        @start-application="handleNavigate('apply')"
      />

      <ProfilePage
        v-else-if="currentView === 'profile'"
        :account="session"
        @back="handleNavigate(defaultView)"
        @logout="handleLogout"
        @navigate="handleNavigate"
      />
    </main>

    <Footer v-if="isLoggedIn" />
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import Navbar from "./components/Navbar.vue";
import Footer from "./components/Footer.vue";
import AdminDashboard from "./components/AdminDashboard.vue";
import HomePage from "./components/HomePage.vue";
import ApplyPage from "./components/ApplyPage.vue";
import StatusPage from "./components/StatusPage.vue";
import ListPage from "./components/ListPage.vue";
import TutorialPage from "./components/TutorialPage.vue";
import LoginPage from "./components/LoginPage.vue";
import ProfilePage from "./components/ProfilePage.vue";
import { clearSession, readSession, saveSession } from "./services/auth";

const ADMIN_VIEWS = new Set(["admin", "list", "status", "profile"]);
const USER_VIEWS = new Set([
  "home",
  "apply",
  "status",
  "list",
  "tutorial",
  "profile",
]);

const session = ref(readSession());
const defaultView = computed(() =>
  session.value?.role === "admin" ? "admin" : "home",
);
const currentView = ref(session.value ? defaultView.value : "login");
const isLoggedIn = computed(() => Boolean(session.value));
const selectedApplicationId = ref(null);

function handleNavigate(view) {
  if (!session.value) {
    currentView.value = "login";
    return;
  }

  const allowedViews = session.value.role === "admin" ? ADMIN_VIEWS : USER_VIEWS;
  currentView.value = allowedViews.has(view) ? view : defaultView.value;
}

function handleLoggedIn(account) {
  session.value = account;
  saveSession(account);
  selectedApplicationId.value = null;
  currentView.value = account.role === "admin" ? "admin" : "home";
}

function handleLogout() {
  clearSession();
  session.value = null;
  selectedApplicationId.value = null;
  currentView.value = "login";
}

function handleSubmitted(applicationId) {
  selectedApplicationId.value = applicationId;
  localStorage.setItem("latestApplicationId", String(applicationId));
  currentView.value = "status";
}

function handleViewDetail(applicationId) {
  selectedApplicationId.value = applicationId;
  localStorage.setItem("latestApplicationId", String(applicationId));
  currentView.value = "status";
}
</script>
