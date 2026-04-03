import { isAuthenticated, handleLogin, handleLogout, unauthorizedResponse } from "./auth";
import { handleApiRequest } from "./api";
import { getDashboardHtml } from "./ui";
import type { Env } from "../index";

/**
 * Handle all HTTP requests to the dashboard.
 */
export async function handleHttpRequest(
  request: Request,
  env: Env
): Promise<Response> {
  const url = new URL(request.url);

  // Login endpoint (unauthenticated)
  if (url.pathname === "/api/login" && request.method === "POST") {
    const body = (await request.json()) as { token?: string };
    return handleLogin(body.token ?? "", env.DASHBOARD_TOKEN);
  }

  // Logout endpoint
  if (url.pathname === "/api/logout" && request.method === "POST") {
    return handleLogout();
  }

  // All other API routes require auth
  if (url.pathname.startsWith("/api/")) {
    if (!isAuthenticated(request, env.DASHBOARD_TOKEN)) {
      return unauthorizedResponse();
    }
    return handleApiRequest(request, env, url);
  }

  // Serve dashboard HTML for root and any non-API path
  if (request.method === "GET") {
    return new Response(getDashboardHtml(), {
      headers: { "Content-Type": "text/html; charset=utf-8" },
    });
  }

  return new Response("Not found", { status: 404 });
}
