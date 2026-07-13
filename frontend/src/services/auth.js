const SESSION_KEY = "financial-crisis-session";

const DEMO_ACCOUNTS = {
  admin: {
    password: "admin123",
    role: "admin",
    displayName: "系统管理员",
  },
  user: {
    password: "user123",
    role: "user",
    displayName: "贷款客户",
  },
};

export function authenticate(username, password) {
  const normalizedUsername = String(username || "").trim().toLowerCase();
  const account = DEMO_ACCOUNTS[normalizedUsername];

  if (!account || account.password !== password) {
    return null;
  }

  return {
    username: normalizedUsername,
    role: account.role,
    displayName: account.displayName,
  };
}

export function readSession() {
  try {
    const session = JSON.parse(localStorage.getItem(SESSION_KEY));
    if (
      session &&
      DEMO_ACCOUNTS[session.username]?.role === session.role
    ) {
      return session;
    }
  } catch {
    // Ignore invalid or manually edited browser storage.
  }
  return null;
}

export function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}
