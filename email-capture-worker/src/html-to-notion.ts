import TurndownService from "turndown";
import { parseHTML } from "linkedom";

// ── Types ──────────────────────────────────────────────────────────────────

interface RichTextObject {
  type: "text";
  text: { content: string; link?: { url: string } };
  annotations?: {
    bold?: boolean;
    italic?: boolean;
    code?: boolean;
    strikethrough?: boolean;
  };
}

export interface NotionBlock {
  object: "block";
  type: string;
  [key: string]: unknown;
}

// ── HTML Preprocessing ─────────────────────────────────────────────────────

/**
 * Clean up newsletter HTML before Markdown conversion:
 * - Strip <style> and <script> tags
 * - Strip tracking pixels (1x1 images)
 * - Strip common unsubscribe/footer sections
 */
function preprocessHtml(html: string): string {
  let cleaned = html;

  // Remove <style> and <script> blocks
  cleaned = cleaned.replace(/<(style|script)[^>]*>[\s\S]*?<\/\1>/gi, "");

  // Remove tracking pixels (images with width/height of 1)
  cleaned = cleaned.replace(
    /<img[^>]*(?:width\s*=\s*["']?1["']?\s|height\s*=\s*["']?1["']?\s)[^>]*\/?>/gi,
    ""
  );

  // Remove HTML comments
  cleaned = cleaned.replace(/<!--[\s\S]*?-->/g, "");

  return cleaned;
}

// ── HTML → Markdown ────────────────────────────────────────────────────────

/**
 * Convert HTML to Markdown using Turndown with linkedom as the DOM parser.
 */
export function convertHtmlToMarkdown(html: string): string {
  const cleaned = preprocessHtml(html);

  // Provide a full DOM environment for Turndown via linkedom.
  // Turndown calls document.implementation.createHTMLDocument(), so we need
  // to set up globalThis.document with a linkedom document that supports it.
  const { document, HTMLElement } = parseHTML(
    "<!DOCTYPE html><html><body></body></html>"
  );

  // Patch createHTMLDocument if linkedom doesn't provide it
  if (document.implementation && !document.implementation.createHTMLDocument) {
    (document.implementation as any).createHTMLDocument = (title?: string) => {
      const { document: newDoc } = parseHTML(
        `<!DOCTYPE html><html><head><title>${title ?? ""}</title></head><body></body></html>`
      );
      return newDoc;
    };
  }

  (globalThis as any).document = document;
  (globalThis as any).HTMLElement = HTMLElement ?? class {};
  (globalThis as any).Node = (document as any).defaultView?.Node ?? class {};

  const turndown = new TurndownService({
    headingStyle: "atx",
    codeBlockStyle: "fenced",
    bulletListMarker: "-",
  });

  // Remove empty links and image-only links that are just newsletter chrome
  turndown.addRule("removeEmptyLinks", {
    filter: (node) =>
      node.nodeName === "A" &&
      !node.textContent?.trim() &&
      !node.querySelector?.("img"),
    replacement: () => "",
  });

  const markdown = turndown.turndown(cleaned);

  // Clean up globalThis
  delete (globalThis as any).document;
  delete (globalThis as any).HTMLElement;
  delete (globalThis as any).Node;

  // Collapse excessive blank lines
  return markdown.replace(/\n{3,}/g, "\n\n").trim();
}

// ── Rich Text Parsing ──────────────────────────────────────────────────────

const RICH_TEXT_MAX_LENGTH = 2000;

/**
 * Parse inline Markdown formatting into Notion rich_text objects.
 * Handles: **bold**, *italic*, `code`, [links](url), ~~strikethrough~~
 */
function parseRichText(text: string): RichTextObject[] {
  const results: RichTextObject[] = [];

  // Pattern matches inline formatting tokens in order
  const pattern =
    /(\*\*(.+?)\*\*)|(\*(.+?)\*)|(`(.+?)`)|(~~(.+?)~~)|(\[([^\]]+)\]\(([^)]+)\))/g;

  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = pattern.exec(text)) !== null) {
    // Add plain text before this match
    if (match.index > lastIndex) {
      pushTextSegments(results, text.slice(lastIndex, match.index));
    }

    if (match[1]) {
      // **bold**
      pushTextSegments(results, match[2], { bold: true });
    } else if (match[3]) {
      // *italic*
      pushTextSegments(results, match[4], { italic: true });
    } else if (match[5]) {
      // `code`
      pushTextSegments(results, match[6], { code: true });
    } else if (match[7]) {
      // ~~strikethrough~~
      pushTextSegments(results, match[8], { strikethrough: true });
    } else if (match[9]) {
      // [link text](url)
      pushTextSegments(results, match[10], undefined, match[11]);
    }

    lastIndex = match.index + match[0].length;
  }

  // Remaining plain text
  if (lastIndex < text.length) {
    pushTextSegments(results, text.slice(lastIndex));
  }

  // If nothing was parsed, return plain text
  if (results.length === 0 && text.length > 0) {
    pushTextSegments(results, text);
  }

  return results;
}

/**
 * Push text segments, splitting at RICH_TEXT_MAX_LENGTH boundaries.
 */
function pushTextSegments(
  results: RichTextObject[],
  content: string,
  annotations?: RichTextObject["annotations"],
  linkUrl?: string
): void {
  // Split long content at word boundaries
  let remaining = content;
  while (remaining.length > RICH_TEXT_MAX_LENGTH) {
    let splitAt = remaining.lastIndexOf(" ", RICH_TEXT_MAX_LENGTH);
    if (splitAt <= 0) splitAt = RICH_TEXT_MAX_LENGTH;
    const chunk = remaining.slice(0, splitAt);
    remaining = remaining.slice(splitAt).trimStart();

    const obj: RichTextObject = {
      type: "text",
      text: { content: chunk },
    };
    if (linkUrl) obj.text.link = { url: linkUrl };
    if (annotations) obj.annotations = annotations;
    results.push(obj);
  }

  if (remaining.length > 0) {
    const obj: RichTextObject = {
      type: "text",
      text: { content: remaining },
    };
    if (linkUrl) obj.text.link = { url: linkUrl };
    if (annotations) obj.annotations = annotations;
    results.push(obj);
  }
}

// ── Markdown → Notion Blocks ──────────────────────────────────────────────

function makeBlock(type: string, richText: RichTextObject[]): NotionBlock {
  return {
    object: "block",
    type,
    [type]: { rich_text: richText },
  };
}

function makeDivider(): NotionBlock {
  return { object: "block", type: "divider", divider: {} };
}

function makeImage(url: string): NotionBlock {
  return {
    object: "block",
    type: "image",
    image: { type: "external", external: { url } },
  };
}

/**
 * Convert a Markdown string into an array of Notion block objects.
 */
export function markdownToNotionBlocks(markdown: string): NotionBlock[] {
  const blocks: NotionBlock[] = [];
  const lines = markdown.split("\n");

  let i = 0;
  while (i < lines.length) {
    const line = lines[i];

    // Skip empty lines
    if (line.trim() === "") {
      i++;
      continue;
    }

    // Horizontal rule
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(line.trim())) {
      blocks.push(makeDivider());
      i++;
      continue;
    }

    // Headings
    const headingMatch = line.match(/^(#{1,3})\s+(.+)/);
    if (headingMatch) {
      const level = headingMatch[1].length;
      const type =
        level === 1
          ? "heading_1"
          : level === 2
            ? "heading_2"
            : "heading_3";
      blocks.push(makeBlock(type, parseRichText(headingMatch[2])));
      i++;
      continue;
    }

    // Fenced code block
    if (line.trimStart().startsWith("```")) {
      const lang = line.trimStart().slice(3).trim();
      const codeLines: string[] = [];
      i++;
      while (i < lines.length && !lines[i].trimStart().startsWith("```")) {
        codeLines.push(lines[i]);
        i++;
      }
      i++; // skip closing ```
      blocks.push({
        object: "block",
        type: "code",
        code: {
          rich_text: [{ type: "text", text: { content: codeLines.join("\n") } }],
          language: lang || "plain text",
        },
      });
      continue;
    }

    // Blockquote
    if (line.trimStart().startsWith("> ")) {
      const quoteLines: string[] = [];
      while (i < lines.length && lines[i].trimStart().startsWith("> ")) {
        quoteLines.push(lines[i].replace(/^>\s?/, ""));
        i++;
      }
      blocks.push(makeBlock("quote", parseRichText(quoteLines.join(" "))));
      continue;
    }

    // Bulleted list item
    const bulletMatch = line.match(/^(\s*)[-*+]\s+(.+)/);
    if (bulletMatch) {
      blocks.push(
        makeBlock("bulleted_list_item", parseRichText(bulletMatch[2]))
      );
      i++;
      continue;
    }

    // Numbered list item
    const numberedMatch = line.match(/^(\s*)\d+\.\s+(.+)/);
    if (numberedMatch) {
      blocks.push(
        makeBlock("numbered_list_item", parseRichText(numberedMatch[2]))
      );
      i++;
      continue;
    }

    // Image (standalone on a line)
    const imageMatch = line.trim().match(/^!\[([^\]]*)\]\(([^)]+)\)$/);
    if (imageMatch) {
      blocks.push(makeImage(imageMatch[2]));
      i++;
      continue;
    }

    // Default: paragraph — accumulate consecutive non-empty, non-special lines
    const paraLines: string[] = [line];
    i++;
    while (
      i < lines.length &&
      lines[i].trim() !== "" &&
      !lines[i].match(/^#{1,3}\s/) &&
      !lines[i].trimStart().startsWith("```") &&
      !lines[i].trimStart().startsWith("> ") &&
      !lines[i].match(/^\s*[-*+]\s/) &&
      !lines[i].match(/^\s*\d+\.\s/) &&
      !/^(-{3,}|\*{3,}|_{3,})$/.test(lines[i].trim())
    ) {
      paraLines.push(lines[i]);
      i++;
    }

    blocks.push(makeBlock("paragraph", parseRichText(paraLines.join(" "))));
  }

  return blocks;
}
