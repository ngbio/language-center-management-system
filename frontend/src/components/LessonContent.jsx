import { createElement, useMemo } from "react";

const allowedTags = new Set([
  "div", "p", "br", "hr", "h1", "h2", "h3", "h4", "h5", "h6",
  "strong", "b", "em", "i", "u", "s", "span", "small", "sub", "sup",
  "blockquote", "code", "pre", "ul", "ol", "li", "dl", "dt", "dd",
  "table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption",
  "ruby", "rt", "rp",
]);
const omittedTags = new Set([
  "script", "style", "iframe", "object", "embed", "template", "svg", "math",
]);

function renderNode(node, key) {
  if (node.nodeType === 3) return node.textContent;
  if (node.nodeType !== 1) return null;
  const tag = node.localName;
  if (omittedTags.has(tag)) return null;
  const children = Array.from(node.childNodes, (child, index) => renderNode(child, `${key}-${index}`));
  // Rebuild formatting elements without copying attributes, URLs or event handlers.
  if (!allowedTags.has(tag)) return children;
  if (tag === "br" || tag === "hr") return createElement(tag, { key });
  return createElement(tag, { key }, children);
}

export default function LessonContent({ html }) {
  const content = useMemo(() => {
    const documentNode = new DOMParser().parseFromString(html || "", "text/html");
    return Array.from(documentNode.body.childNodes, (node, index) => renderNode(node, String(index)));
  }, [html]);

  return <div className="lesson-content">{content}</div>;
}
