export interface ProcessingLogEntry {
  id: number;
  created_at: string;
  sender_name: string;
  sender_address: string;
  subject: string;
  recipient: string;
  destination: string;
  database_id: string;
  tag: string | null;
  format: string | null;
  notion_page_id: string | null;
  status: "success" | "error";
  error_message: string | null;
}

export interface LogInsert {
  sender_name: string;
  sender_address: string;
  subject: string;
  recipient: string;
  destination: string;
  database_id: string;
  tag: string | null;
  format: string | null;
  notion_page_id: string | null;
  status: "success" | "error";
  error_message: string | null;
  raw_email: ArrayBuffer | null;
}

export interface LogFilters {
  status?: string;
  destination?: string;
  limit?: number;
  offset?: number;
}

/**
 * Insert a processing log entry.
 */
export async function insertLog(db: D1Database, entry: LogInsert): Promise<void> {
  await db
    .prepare(
      `INSERT INTO processing_log
        (sender_name, sender_address, subject, recipient, destination, database_id, tag, format, notion_page_id, status, error_message, raw_email)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      entry.sender_name,
      entry.sender_address,
      entry.subject,
      entry.recipient,
      entry.destination,
      entry.database_id,
      entry.tag,
      entry.format,
      entry.notion_page_id,
      entry.status,
      entry.error_message,
      entry.raw_email
    )
    .run();
}

/**
 * Fetch processing log entries with optional filters.
 */
export async function getLogs(
  db: D1Database,
  filters: LogFilters = {}
): Promise<{ results: ProcessingLogEntry[]; total: number }> {
  const conditions: string[] = [];
  const params: unknown[] = [];

  if (filters.status) {
    conditions.push("status = ?");
    params.push(filters.status);
  }
  if (filters.destination) {
    conditions.push("destination = ?");
    params.push(filters.destination);
  }

  const where = conditions.length > 0 ? `WHERE ${conditions.join(" AND ")}` : "";
  const limit = filters.limit ?? 50;
  const offset = filters.offset ?? 0;

  const countResult = await db
    .prepare(`SELECT COUNT(*) as total FROM processing_log ${where}`)
    .bind(...params)
    .first<{ total: number }>();

  const results = await db
    .prepare(
      `SELECT id, created_at, sender_name, sender_address, subject, recipient,
              destination, database_id, tag, format, notion_page_id, status, error_message
       FROM processing_log ${where}
       ORDER BY created_at DESC
       LIMIT ? OFFSET ?`
    )
    .bind(...params, limit, offset)
    .all<ProcessingLogEntry>();

  return {
    results: results.results ?? [],
    total: countResult?.total ?? 0,
  };
}

/**
 * Get the raw email bytes for a specific log entry (for retry).
 */
export async function getRawEmail(
  db: D1Database,
  logId: number
): Promise<ArrayBuffer | null> {
  const row = await db
    .prepare("SELECT raw_email FROM processing_log WHERE id = ?")
    .bind(logId)
    .first<{ raw_email: ArrayBuffer | null }>();
  return row?.raw_email ?? null;
}

/**
 * Update a log entry after a retry.
 */
export async function updateLogAfterRetry(
  db: D1Database,
  logId: number,
  notionPageId: string | null,
  status: "success" | "error",
  errorMessage: string | null
): Promise<void> {
  await db
    .prepare(
      `UPDATE processing_log SET notion_page_id = ?, status = ?, error_message = ? WHERE id = ?`
    )
    .bind(notionPageId, status, errorMessage, logId)
    .run();
}

/**
 * Get analytics data: counts by day, destination, and status.
 */
export async function getStats(
  db: D1Database,
  days: number = 30
): Promise<{
  daily: { date: string; count: number }[];
  byDestination: { destination: string; count: number }[];
  byStatus: { status: string; count: number }[];
  topSources: { sender_name: string; count: number }[];
  topTags: { tag: string; count: number }[];
  total: number;
}> {
  const since = new Date(Date.now() - days * 86400000).toISOString();

  const [daily, byDestination, byStatus, topSources, topTags, totalResult] =
    await Promise.all([
      db
        .prepare(
          `SELECT date(created_at) as date, COUNT(*) as count
           FROM processing_log WHERE created_at >= ?
           GROUP BY date(created_at) ORDER BY date`
        )
        .bind(since)
        .all<{ date: string; count: number }>(),
      db
        .prepare(
          `SELECT destination, COUNT(*) as count
           FROM processing_log WHERE created_at >= ?
           GROUP BY destination`
        )
        .bind(since)
        .all<{ destination: string; count: number }>(),
      db
        .prepare(
          `SELECT status, COUNT(*) as count
           FROM processing_log WHERE created_at >= ?
           GROUP BY status`
        )
        .bind(since)
        .all<{ status: string; count: number }>(),
      db
        .prepare(
          `SELECT sender_name, COUNT(*) as count
           FROM processing_log WHERE created_at >= ?
           GROUP BY sender_name ORDER BY count DESC LIMIT 10`
        )
        .bind(since)
        .all<{ sender_name: string; count: number }>(),
      db
        .prepare(
          `SELECT tag, COUNT(*) as count
           FROM processing_log WHERE created_at >= ? AND tag IS NOT NULL
           GROUP BY tag ORDER BY count DESC LIMIT 10`
        )
        .bind(since)
        .all<{ tag: string; count: number }>(),
      db
        .prepare(
          `SELECT COUNT(*) as total FROM processing_log WHERE created_at >= ?`
        )
        .bind(since)
        .first<{ total: number }>(),
    ]);

  return {
    daily: daily.results ?? [],
    byDestination: byDestination.results ?? [],
    byStatus: byStatus.results ?? [],
    topSources: topSources.results ?? [],
    topTags: topTags.results ?? [],
    total: totalResult?.total ?? 0,
  };
}

/**
 * Get a config value, parsed as JSON.
 */
export async function getConfig<T>(db: D1Database, key: string): Promise<T | null> {
  const row = await db
    .prepare("SELECT value FROM config WHERE key = ?")
    .bind(key)
    .first<{ value: string }>();
  if (!row) return null;
  return JSON.parse(row.value) as T;
}

/**
 * Set a config value (stored as JSON string).
 */
export async function setConfig(
  db: D1Database,
  key: string,
  value: unknown
): Promise<void> {
  await db
    .prepare(
      `INSERT INTO config (key, value, updated_at) VALUES (?, ?, datetime('now'))
       ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at`
    )
    .bind(key, JSON.stringify(value))
    .run();
}

/**
 * Get all config entries as a key-value map.
 */
export async function getAllConfig(db: D1Database): Promise<Record<string, unknown>> {
  const rows = await db
    .prepare("SELECT key, value FROM config")
    .all<{ key: string; value: string }>();

  const config: Record<string, unknown> = {};
  for (const row of rows.results ?? []) {
    config[row.key] = JSON.parse(row.value);
  }
  return config;
}
