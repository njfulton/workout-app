import {
  getLogs,
  getStats,
  getRawEmail,
  updateLogAfterRetry,
  getAllConfig,
  setConfig,
} from "../db";
import { parseEmail } from "../email-parser";
import { resolveRouting } from "../address-router";
import { convertHtmlToMarkdown, markdownToNotionBlocks } from "../html-to-notion";
import { createNotionPage } from "../notion";
import type { Env } from "../index";

/**
 * Route API requests to the appropriate handler.
 */
export async function handleApiRequest(
  request: Request,
  env: Env,
  url: URL
): Promise<Response> {
  const path = url.pathname.replace(/^\/api/, "");
  const method = request.method;

  // GET /api/logs — fetch processing log
  if (method === "GET" && path === "/logs") {
    const status = url.searchParams.get("status") ?? undefined;
    const destination = url.searchParams.get("destination") ?? undefined;
    const limit = parseInt(url.searchParams.get("limit") ?? "50", 10);
    const offset = parseInt(url.searchParams.get("offset") ?? "0", 10);

    const data = await getLogs(env.DB, { status, destination, limit, offset });
    return Response.json(data);
  }

  // GET /api/stats — analytics data
  if (method === "GET" && path === "/stats") {
    const days = parseInt(url.searchParams.get("days") ?? "30", 10);
    const data = await getStats(env.DB, days);
    return Response.json(data);
  }

  // POST /api/retry/:id — retry a failed email
  if (method === "POST" && path.startsWith("/retry/")) {
    const logId = parseInt(path.split("/")[2], 10);
    if (isNaN(logId)) {
      return Response.json({ error: "Invalid log ID" }, { status: 400 });
    }

    const rawEmail = await getRawEmail(env.DB, logId);
    if (!rawEmail) {
      return Response.json(
        { error: "No raw email stored for this entry" },
        { status: 404 }
      );
    }

    try {
      // Re-process the email
      const email = await parseEmail(rawEmail);

      // Get the original log entry to find the recipient
      const logs = await getLogs(env.DB, { limit: 1, offset: 0 });
      const logEntry = logs.results.find((l) => l.id === logId);
      if (!logEntry) {
        return Response.json({ error: "Log entry not found" }, { status: 404 });
      }

      const routing = resolveRouting(logEntry.recipient, env);
      const content = email.htmlBody || email.textBody;
      const blocks = markdownToNotionBlocks(convertHtmlToMarkdown(content));
      const pageId = await createNotionPage(email, routing, blocks, env.NOTION_API_KEY);

      await updateLogAfterRetry(env.DB, logId, pageId, "success", null);
      return Response.json({ ok: true, pageId });
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      await updateLogAfterRetry(env.DB, logId, null, "error", message);
      return Response.json({ error: message }, { status: 500 });
    }
  }

  // GET /api/config — get all config
  if (method === "GET" && path === "/config") {
    const config = await getAllConfig(env.DB);
    return Response.json(config);
  }

  // PUT /api/config/:key — update a config value
  if (method === "PUT" && path.startsWith("/config/")) {
    const key = decodeURIComponent(path.slice("/config/".length));
    const body = await request.json();
    await setConfig(env.DB, key, body);
    return Response.json({ ok: true });
  }

  return Response.json({ error: "Not found" }, { status: 404 });
}
