import { parseEmail } from "./email-parser";
import { resolveRouting } from "./address-router";
import { convertHtmlToMarkdown, markdownToNotionBlocks } from "./html-to-notion";
import { createNotionPage } from "./notion";
import { insertLog } from "./db";
import { handleHttpRequest } from "./dashboard/index";

export interface Env {
  NOTION_API_KEY: string;
  DASHBOARD_TOKEN: string;
  NOTES_DB_ID: string;
  CAPTURE_DB_ID: string;
  TASKS_DB_ID: string;
  DB: D1Database;
}

export default {
  /**
   * Handle inbound emails from Cloudflare Email Routing.
   */
  async email(
    message: ForwardableEmailMessage,
    env: Env,
    _ctx: ExecutionContext
  ): Promise<void> {
    // Read raw email bytes (store for potential retry)
    const rawEmail = await new Response(message.raw).arrayBuffer();

    let email;
    let routing;

    try {
      // 1. Parse email
      email = await parseEmail(rawEmail);
      console.log(
        `Processing email: "${email.subject}" from ${email.fromName} (${email.fromAddress})`
      );

      // 2. Determine routing from recipient address
      routing = resolveRouting(message.to, env);
      console.log(
        `Routing to ${routing.destination} (DB: ${routing.databaseId})` +
          (routing.tag ? `, tag: ${routing.tag}` : "") +
          `, format: ${routing.format}`
      );

      // 3. Convert email body to Notion blocks
      const content = email.htmlBody || email.textBody;
      let blocks = markdownToNotionBlocks(convertHtmlToMarkdown(content));

      if (blocks.length === 0) {
        blocks = [
          {
            object: "block",
            type: "paragraph",
            paragraph: {
              rich_text: [
                { type: "text", text: { content: "(Email had no body content)" } },
              ],
            },
          },
        ];
      }

      // 4. Create page in Notion
      const pageId = await createNotionPage(email, routing, blocks, env.NOTION_API_KEY);
      console.log(`Created Notion page: ${pageId}`);

      // 5. Log success to D1
      await insertLog(env.DB, {
        sender_name: email.fromName,
        sender_address: email.fromAddress,
        subject: email.subject,
        recipient: message.to,
        destination: routing.destination,
        database_id: routing.databaseId,
        tag: routing.tag,
        format: routing.format,
        notion_page_id: pageId,
        status: "success",
        error_message: null,
        raw_email: rawEmail,
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error("Failed to process email:", errorMessage);

      // Log failure to D1
      try {
        await insertLog(env.DB, {
          sender_name: email?.fromName ?? "unknown",
          sender_address: email?.fromAddress ?? "unknown",
          subject: email?.subject ?? "(parse failed)",
          recipient: message.to,
          destination: routing?.destination ?? "unknown",
          database_id: routing?.databaseId ?? "",
          tag: routing?.tag ?? null,
          format: routing?.format ?? null,
          notion_page_id: null,
          status: "error",
          error_message: errorMessage,
          raw_email: rawEmail,
        });
      } catch (logError) {
        console.error("Failed to log error to D1:", logError);
      }
    }
  },

  /**
   * Handle HTTP requests — serves the dashboard UI and API.
   */
  async fetch(
    request: Request,
    env: Env,
    _ctx: ExecutionContext
  ): Promise<Response> {
    return handleHttpRequest(request, env);
  },
};
