import type { Metadata } from "next";
import Link from "next/link";

import BorderGlow from "@/components/site/border-glow";
import { Button } from "@/components/ui/button";
import { Eyebrow, PageTitle, SampleBadge, Section } from "@/components/site/primitives";
import { BlogList } from "@/components/site/blog-list";
import { featuredPost, posts } from "@/lib/content/blog";

export const metadata: Metadata = {
  title: "Blog",
  description:
    "Editorials and write-ups from Programming Club @ DAU contests, published by members.",
};

export default function BlogPage() {
  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <Eyebrow>Blog</Eyebrow>
            <PageTitle className="max-w-[20ch]">
              Editorials and write-ups.
            </PageTitle>
          </div>
          <SampleBadge>Sample content</SampleBadge>
        </div>
      </Section>

      <Section className="pb-10">
        {/* The article element stays: BorderGlow renders a div, and the
            featured post is a self-contained piece of content. */}
        <article>
          <BorderGlow contentClassName="grid gap-8 p-9 md:grid-cols-2 md:items-center">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full border border-primary px-2.5 py-1 font-mono text-[10px] tracking-[0.12em] text-primary uppercase">
                Featured
              </span>
              <span className="font-mono text-[11px] text-fg-subtle">
                {featuredPost.date} · {featuredPost.read}
              </span>
            </div>
            <h2 className="mt-5 text-[clamp(1.375rem,2.8vw,1.75rem)] font-semibold tracking-[-0.02em] text-pretty">
              {featuredPost.title}
            </h2>
            <p className="mt-3 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
              {featuredPost.excerpt}
            </p>
            <div className="mt-6 flex items-center gap-2.5">
              <span className="flex size-7 items-center justify-center rounded-full border border-border bg-surface-2 font-mono text-[11px] text-fg-muted">
                {featuredPost.initials}
              </span>
              <span className="font-mono text-xs text-fg-muted">{featuredPost.author}</span>
            </div>
          </div>

          <pre className="overflow-x-auto rounded-control border border-hairline bg-surface-2 p-5 font-mono text-xs leading-5 text-fg-muted">
            <code>
              <span className="text-fg-subtle">{"// solution sketch"}</span>
              {"\n"}
              <span className="text-cf-expert">for</span>
              {" (int i = 1; i <= n; i++)\n"}
              {"    dp[i] = min(dp[i-1] + a[i], best);\n"}
              <span className="text-cf-candidate">cout</span>
              {" << dp[n] << "}
              <span className="text-cf-pupil">{"'\\n'"}</span>
              {";\n\n"}
              <span className="text-fg-subtle">{"// O(n) time, O(1) extra space"}</span>
            </code>
          </pre>
          </BorderGlow>
        </article>
      </Section>

      <Section className="pb-10">
        <BlogList posts={posts} />
      </Section>

      <Section className="pb-22">
        <div className="flex flex-wrap items-center justify-between gap-5 rounded-panel border border-hairline bg-surface p-8">
          <div>
            <p className="text-lg font-semibold tracking-tight">Write for the club blog</p>
            <p className="mt-2 max-w-[48ch] text-[15px] leading-[1.5] text-fg-muted text-pretty">
              Members can publish editorials after any club round. Drafts are reviewed by
              the core team before they go live.
            </p>
          </div>
          <Button asChild className="h-10 rounded-full px-5.5">
            <Link href="/login">Start a draft</Link>
          </Button>
        </div>
      </Section>
    </>
  );
}
