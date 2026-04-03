import PostalMime from "postal-mime";

export interface ParsedEmail {
  subject: string;
  fromName: string;
  fromAddress: string;
  date: string; // YYYY-MM-DD
  htmlBody: string;
  textBody: string;
  viewInBrowserUrl: string | null;
}

/**
 * Scan HTML for common "View in browser" / "View online" links
 * and return the first matching URL.
 */
function extractViewInBrowserUrl(html: string): string | null {
  // Match anchor tags whose visible text suggests a "view in browser" link
  const pattern =
    /<a[^>]+href=["']([^"']+)["'][^>]*>[^<]*(?:view\s+(?:in\s+(?:your\s+)?browser|online|this\s+email)|read\s+(?:in\s+browser|online))[^<]*<\/a>/gi;
  const match = pattern.exec(html);
  return match ? match[1] : null;
}

/**
 * Parse a raw RFC 5322 email (as ArrayBuffer) into structured fields.
 */
export async function parseEmail(rawEmail: ArrayBuffer): Promise<ParsedEmail> {
  const parser = new PostalMime();
  const parsed = await parser.parse(rawEmail);

  // Normalize sender name: fall back to local part of address if no display name
  const fromAddress = parsed.from?.address ?? "";
  let fromName = parsed.from?.name ?? "";
  if (!fromName && fromAddress) {
    fromName = fromAddress.split("@")[0];
  }

  // Normalize date to YYYY-MM-DD
  let date: string;
  if (parsed.date) {
    const d = new Date(parsed.date);
    date = isNaN(d.getTime())
      ? new Date().toISOString().split("T")[0]
      : d.toISOString().split("T")[0];
  } else {
    date = new Date().toISOString().split("T")[0];
  }

  const htmlBody = parsed.html ?? "";
  const textBody = parsed.text ?? "";

  const viewInBrowserUrl = htmlBody ? extractViewInBrowserUrl(htmlBody) : null;

  return {
    subject: parsed.subject ?? "(No subject)",
    fromName,
    fromAddress,
    date,
    htmlBody,
    textBody,
    viewInBrowserUrl,
  };
}
