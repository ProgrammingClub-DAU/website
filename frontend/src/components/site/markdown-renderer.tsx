"use client";

import React, { useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import rehypeKatex from "rehype-katex";
import "katex/dist/katex.min.css";
import { Check, Copy } from "lucide-react";

interface MarkdownRendererProps {
  content: string;
  className?: string;
}

export function MarkdownRenderer({ content, className = "" }: MarkdownRendererProps) {
  return (
    <div className={`prose prose-invert max-w-none text-foreground text-sm leading-relaxed ${className}`}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[rehypeKatex]}
        components={{
          h1: ({ children }) => (
            <h1 className="text-2xl font-bold tracking-tight text-foreground mt-8 mb-4 border-b border-foreground/10 pb-2">
              {children}
            </h1>
          ),
          h2: ({ children }) => (
            <h2 className="text-xl font-bold tracking-tight text-foreground mt-6 mb-3 border-b border-foreground/5 pb-1">
              {children}
            </h2>
          ),
          h3: ({ children }) => (
            <h3 className="text-lg font-bold text-foreground mt-5 mb-2">
              {children}
            </h3>
          ),
          p: ({ children }) => (
            <p className="my-3 text-fg-muted leading-7">
              {children}
            </p>
          ),
          a: ({ href, children }) => {
            const isExternal = href?.startsWith("http");
            return (
              <a
                href={href}
                target={isExternal ? "_blank" : undefined}
                rel={isExternal ? "noreferrer noopener" : undefined}
                className="text-primary hover:underline font-medium transition-colors"
              >
                {children}
              </a>
            );
          },
          blockquote: ({ children }) => (
            <blockquote className="my-4 border-l-2 border-primary/60 bg-foreground/[0.02] py-2 px-4 italic text-fg-muted rounded-r-xl">
              {children}
            </blockquote>
          ),
          ul: ({ children }) => (
            <ul className="my-3 list-disc pl-5 space-y-1 text-fg-muted">
              {children}
            </ul>
          ),
          ol: ({ children }) => (
            <ol className="my-3 list-decimal pl-5 space-y-1 text-fg-muted">
              {children}
            </ol>
          ),
          table: ({ children }) => (
            <div className="my-5 overflow-x-auto rounded-xl border border-foreground/10 bg-card/70 shadow-md">
              <table className="w-full text-left text-xs border-collapse">
                {children}
              </table>
            </div>
          ),
          th: ({ children }) => (
            <th className="py-2.5 px-3 border-b border-foreground/10 bg-foreground/[0.03] font-mono uppercase tracking-caps text-nano text-fg-muted">
              {children}
            </th>
          ),
          td: ({ children }) => (
            <td className="py-2.5 px-3 border-b border-foreground/5 text-fg-muted">
              {children}
            </td>
          ),
          code: ({ className, children, ...props }) => {
            const isInline = !className;
            if (isInline) {
              return (
                <code
                  className="rounded bg-foreground/10 px-1.5 py-0.5 font-mono text-[13px] text-amber-300"
                  {...props}
                >
                  {children}
                </code>
              );
            }
            return <CodeBlock className={className}>{String(children)}</CodeBlock>;
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}

function CodeBlock({ children, className }: { children: string; className?: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(children.replace(/\n$/, ""));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const language = className?.replace(/language-/, "") || "";

  return (
    <div className="relative my-4 rounded-xl border border-foreground/10 bg-surface-3 overflow-hidden group">
      <div className="flex items-center justify-between border-b border-foreground/5 bg-foreground/[0.02] px-4 py-1.5 text-nano font-mono text-fg-subtle">
        <span>{language ? language.toUpperCase() : "CODE"}</span>
        <button
          type="button"
          onClick={handleCopy}
          className="inline-flex items-center gap-1 hover:text-foreground transition-colors"
          title="Copy code"
        >
          {copied ? <Check className="size-3 text-emerald-400" /> : <Copy className="size-3" />}
          <span>{copied ? "Copied" : "Copy"}</span>
        </button>
      </div>
      <pre className="p-4 overflow-x-auto text-xs font-mono leading-relaxed text-slate-200">
        <code>{children}</code>
      </pre>
    </div>
  );
}
