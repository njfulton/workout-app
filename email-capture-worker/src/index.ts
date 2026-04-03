import { parseEmail } from "./email-parser";
import { resolveRouting } from "./address-router";
import { convertHtmlToMarkdown, markdownToNotionBlocks } from "./html-to-notion";
import { createNotionPage } from "./notion";

export interface Env {
  NOTION_API_KEY: string;
  NOTES_DB_ID: string;
  CAPTURE_DB_ID: string;
  TASKS_DB_ID: string;
}

export default {
  async email(
    message: ForwardableEmailMessage,
    env: Env,
    _ctx: ExecutionContext
  ): Promise<void> {
    try {
      // 1. Read raw email bytes
      const rawEmail = await new Response(message.raw).arrayBuffer();

      // 2. Parse email
      const email = await parseEmail(rawEmail);
      console.log(
        `Processing email: "${email.subject}" from ${email.fromName} (${email.fromAddress})`
      );

      // 3. Determine routing from recipient address
      const routing = resolveRouting(message.to, env);
      console.log(
        `Routing to ${routing.destination} (DB: ${routing.databaseId})` +
          (routing.tag ? `, tag: ${routing.tag}` : "") +
          `, format: ${routing.format}`
      );

      // 4. Convert email body to Notion blocks
      const content = email.htmlBody || email.textBody;
      let blocks = markdownToNotionBlocks(convertHtmlToMarkdown(content));

      // For empty content, add a placeholder
      if (blocks.length === 0) {
        blocks = [
          {
            object: "block",
            type: "paragraph",
            paragraph: {
              rich_text: [
                {
                  type: "text",
                  text: { content: "(Email had no body content)" },
                },
              ],
            },
          },
        ];
      }

      // 5. Create page in Notion
      const pageId = await createNotionPage(email, routing, blocks, env.NOTION_API_KEY);
      console.log(`Created Notion page: ${pageId}`);
    } catch (error) {
      // Log the error but don't reject the email (no bounce-back to sender)
      console.error("Failed to process email:", error);
    }
  },
};
