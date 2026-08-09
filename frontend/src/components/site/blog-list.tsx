"use client";

import { useState } from "react";

import { FilterChips } from "@/components/site/filter-chips";
import { EmptyState } from "@/components/site/primitives";
import { blogTags, type Post } from "@/lib/content/blog";

export function BlogList({ posts }: { posts: Post[] }) {
  const [tag, setTag] = useState("All");
  const visible = tag === "All" ? posts : posts.filter((p) => p.tags.includes(tag));

  return (
    <>
      <FilterChips label="Filter by tag" options={blogTags} value={tag} onChange={setTag} />

      {visible.length === 0 ? (
        <div className="mt-10">
          <EmptyState
            title="No posts with that tag yet."
            hint="Pick another tag, or write the first one."
          />
        </div>
      ) : (
        <div className="mt-10 border-t border-hairline">
          {/*
            Rendered as articles, not links: individual post routes do not
            exist yet, and a link back to this same page is a dead end that
            still advertises itself as clickable.
          */}
          {visible.map((post) => (
            <article
              key={post.title}
              className="grid gap-2 border-b border-hairline px-2 py-6 md:grid-cols-2 md:gap-8"
            >
              <div>
                <div className="font-mono text-[11px] tracking-[0.08em] text-fg-subtle uppercase">
                  {post.date} · {post.read}
                </div>
                <h3 className="mt-2.5 text-lg font-semibold tracking-tight text-pretty">
                  {post.title}
                </h3>
              </div>
              <div className="flex flex-col gap-3">
                <p className="text-[15px] leading-[1.5] text-fg-muted text-pretty">
                  {post.excerpt}
                </p>
                <div className="flex flex-wrap items-center gap-1.5">
                  {post.tags.map((t) => (
                    <span
                      key={t}
                      className="rounded-full border border-hairline px-2 py-0.5 font-mono text-[11px] text-fg-muted"
                    >
                      {t}
                    </span>
                  ))}
                  <span className="ml-1 font-mono text-[11px] text-fg-subtle">
                    {post.author}
                  </span>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </>
  );
}
