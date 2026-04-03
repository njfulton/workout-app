export type Destination = "notes" | "capture" | "tasks";

export interface RoutingResult {
  destination: Destination;
  databaseId: string;
  tag: string | null;
  format: string;
}

interface Env {
  NOTES_DB_ID: string;
  CAPTURE_DB_ID: string;
  TASKS_DB_ID: string;
}

const LOCAL_PART_TO_DESTINATION: Record<string, Destination> = {
  note: "notes",
  notes: "notes",
  capture: "capture",
  task: "tasks",
  tasks: "tasks",
};

const SPECIAL_TAGS: Record<string, { format: string }> = {
  newsletter: { format: "Newsletter" },
};

/**
 * Capitalize a +tag value for use as a Notion Topic.
 * Short tags (2-3 chars) are fully uppercased (ai → AI, pm → PM).
 * Longer tags get first-letter capitalized (leadership → Leadership).
 */
export function capitalizeTag(tag: string): string {
  if (tag.length <= 3) {
    return tag.toUpperCase();
  }
  return tag.charAt(0).toUpperCase() + tag.slice(1).toLowerCase();
}

/**
 * Parse the recipient email address to determine routing.
 *
 * Supported formats:
 *   note@domain       → Notes DB, Format: Article
 *   note+ai@domain    → Notes DB, Format: Article, Tag: AI
 *   note+newsletter@  → Notes DB, Format: Newsletter
 *   capture@domain    → Capture DB
 *   task@domain       → Tasks DB
 */
export function resolveRouting(recipientAddress: string, env: Env): RoutingResult {
  // Extract local part (before @) and any +tag
  const atIndex = recipientAddress.indexOf("@");
  const localPart = atIndex >= 0 ? recipientAddress.slice(0, atIndex) : recipientAddress;

  let base: string;
  let rawTag: string | null = null;

  const plusIndex = localPart.indexOf("+");
  if (plusIndex >= 0) {
    base = localPart.slice(0, plusIndex).toLowerCase();
    rawTag = localPart.slice(plusIndex + 1).toLowerCase();
    if (rawTag === "") rawTag = null;
  } else {
    base = localPart.toLowerCase();
  }

  // Determine destination from local part, default to notes
  const destination = LOCAL_PART_TO_DESTINATION[base] ?? "notes";

  const dbMap: Record<Destination, string> = {
    notes: env.NOTES_DB_ID,
    capture: env.CAPTURE_DB_ID,
    tasks: env.TASKS_DB_ID,
  };

  // Check for special tag keywords (only applies to notes)
  let format = "Article";
  let tag: string | null = null;

  if (rawTag) {
    if (destination === "notes" && SPECIAL_TAGS[rawTag]) {
      format = SPECIAL_TAGS[rawTag].format;
    } else {
      tag = capitalizeTag(rawTag);
    }
  }

  return {
    destination,
    databaseId: dbMap[destination],
    tag,
    format,
  };
}
