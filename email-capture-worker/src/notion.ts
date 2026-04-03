import type { ParsedEmail } from "./email-parser";
import type { RoutingResult } from "./address-router";
import type { NotionBlock } from "./html-to-notion";

const NOTION_API_URL = "https://api.notion.com/v1";
const NOTION_VERSION = "2022-06-28";
const MAX_BLOCKS_PER_REQUEST = 100;

// ── Property Builders ──────────────────────────────────────────────────────

function richText(content: string) {
  return { rich_text: [{ text: { content } }] };
}

function title(content: string) {
  return { title: [{ text: { content } }] };
}

function select(name: string) {
  return { select: { name } };
}

function multiSelect(names: string[]) {
  return { multi_select: names.map((name) => ({ name })) };
}

function status(name: string) {
  return { status: { name } };
}

function dateProperty(dateStr: string) {
  return { date: { start: dateStr } };
}

function url(urlStr: string) {
  return { url: urlStr };
}

/**
 * Build Notion page properties for the Notes database.
 */
function buildNotesProperties(
  email: ParsedEmail,
  routing: RoutingResult
): Record<string, unknown> {
  const props: Record<string, unknown> = {
    Name: title(email.subject),
    Format: select(routing.format),
    Function: multiSelect(["Consume"]),
    Source: richText(email.fromName),
    Author: richText(email.fromName),
    Status: status("To Read"),
    "Input/Output": select("Input"),
    "Note Date": dateProperty(email.date),
  };

  if (routing.tag) {
    props.Topic = multiSelect([routing.tag]);
  }

  if (email.viewInBrowserUrl) {
    props["URL"] = url(email.viewInBrowserUrl);
  }

  return props;
}

/**
 * Build Notion page properties for the Capture database.
 */
function buildCaptureProperties(
  email: ParsedEmail,
  routing: RoutingResult
): Record<string, unknown> {
  const props: Record<string, unknown> = {
    Name: title(email.subject),
    Status: status("Inbox"),
    Source: richText(email.fromName),
    Author: richText(email.fromName),
  };

  if (email.viewInBrowserUrl) {
    props["URL"] = url(email.viewInBrowserUrl);
  }

  return props;
}

/**
 * Build Notion page properties for the Tasks database.
 */
function buildTasksProperties(
  email: ParsedEmail,
  routing: RoutingResult
): Record<string, unknown> {
  // Build description: prepend tag context if present
  let description = email.textBody?.slice(0, 200) ?? "";
  if (routing.tag) {
    description = `[Tag: ${routing.tag}] ${description}`;
  }

  const props: Record<string, unknown> = {
    Name: title(email.subject),
    Status: status("Inbox"),
  };

  if (description) {
    props.Description = richText(description);
  }

  if (email.viewInBrowserUrl) {
    props["URL"] = url(email.viewInBrowserUrl);
  }

  return props;
}

// ── API Client ─────────────────────────────────────────────────────────────

function buildProperties(
  email: ParsedEmail,
  routing: RoutingResult
): Record<string, unknown> {
  switch (routing.destination) {
    case "notes":
      return buildNotesProperties(email, routing);
    case "capture":
      return buildCaptureProperties(email, routing);
    case "tasks":
      return buildTasksProperties(email, routing);
  }
}

async function notionFetch(
  path: string,
  apiKey: string,
  method: string,
  body: unknown
): Promise<Response> {
  const response = await fetch(`${NOTION_API_URL}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Notion-Version": NOTION_VERSION,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  // Retry once on rate limit
  if (response.status === 429) {
    const retryAfter = parseInt(response.headers.get("Retry-After") ?? "2", 10);
    await new Promise((r) => setTimeout(r, retryAfter * 1000));
    return fetch(`${NOTION_API_URL}${path}`, {
      method,
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Notion-Version": NOTION_VERSION,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });
  }

  return response;
}

/**
 * Create a page in Notion with the given properties and content blocks.
 * Handles the 100-block-per-request limit by batching.
 */
export async function createNotionPage(
  email: ParsedEmail,
  routing: RoutingResult,
  blocks: NotionBlock[],
  apiKey: string
): Promise<string> {
  const properties = buildProperties(email, routing);

  // First request: create the page with up to 100 blocks
  const createBody = {
    parent: { database_id: routing.databaseId },
    properties,
    children: blocks.slice(0, MAX_BLOCKS_PER_REQUEST),
  };

  const createResponse = await notionFetch("/pages", apiKey, "POST", createBody);

  if (!createResponse.ok) {
    const errorText = await createResponse.text();
    throw new Error(
      `Notion API error ${createResponse.status}: ${errorText}`
    );
  }

  const page = (await createResponse.json()) as { id: string };
  const pageId = page.id;

  // Append remaining blocks in batches of 100
  for (let i = MAX_BLOCKS_PER_REQUEST; i < blocks.length; i += MAX_BLOCKS_PER_REQUEST) {
    const batch = blocks.slice(i, i + MAX_BLOCKS_PER_REQUEST);

    const appendResponse = await notionFetch(
      `/blocks/${pageId}/children`,
      apiKey,
      "PATCH",
      { children: batch }
    );

    if (!appendResponse.ok) {
      const errorText = await appendResponse.text();
      console.error(
        `Failed to append blocks batch (offset ${i}): ${appendResponse.status} ${errorText}`
      );
      // Continue with remaining batches rather than failing entirely
    }
  }

  return pageId;
}
