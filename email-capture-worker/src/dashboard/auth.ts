const COOKIE_NAME = "ecw_token";
const COOKIE_MAX_AGE = 60 * 60 * 24 * 30; // 30 days

/**
 * Check if a request is authenticated via cookie or Authorization header.
 */
export function isAuthenticated(request: Request, dashboardToken: string): boolean {
  // Check Authorization header
  const authHeader = request.headers.get("Authorization");
  if (authHeader === `Bearer ${dashboardToken}`) {
    return true;
  }

  // Check cookie
  const cookie = request.headers.get("Cookie") ?? "";
  const match = cookie.match(new RegExp(`${COOKIE_NAME}=([^;]+)`));
  return match?.[1] === dashboardToken;
}

/**
 * Handle login: validate token and set cookie.
 */
export function handleLogin(token: string, dashboardToken: string): Response {
  if (token !== dashboardToken) {
    return Response.json({ error: "Invalid token" }, { status: 401 });
  }

  return new Response(JSON.stringify({ ok: true }), {
    status: 200,
    headers: {
      "Content-Type": "application/json",
      "Set-Cookie": `${COOKIE_NAME}=${dashboardToken}; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=${COOKIE_MAX_AGE}`,
    },
  });
}

/**
 * Handle logout: clear cookie.
 */
export function handleLogout(): Response {
  return new Response(JSON.stringify({ ok: true }), {
    status: 200,
    headers: {
      "Content-Type": "application/json",
      "Set-Cookie": `${COOKIE_NAME}=; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=0`,
    },
  });
}

/**
 * Return a 401 response for unauthenticated API requests.
 */
export function unauthorizedResponse(): Response {
  return Response.json({ error: "Unauthorized" }, { status: 401 });
}
