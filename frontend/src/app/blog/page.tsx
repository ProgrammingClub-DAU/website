import type { Metadata } from "next";

import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { InDevelopment } from "@/components/site/in-development";

export const metadata: Metadata = {
  title: "Blog",
  description:
    "Editorials and write-ups from Programming Club @ DAU contests. In development.",
};

/**
 * The blog is not built yet, so this page says so and nothing more.
 *
 * It used to render a post list and a "Start a draft" button over an empty
 * content file -- the layout of a working blog with nothing behind it. Phase 3
 * builds the real one: posts stored in the database, a review step, comments.
 */
export default function BlogPage() {
  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <Eyebrow>Blog</Eyebrow>
        <PageTitle className="max-w-[20ch]">Editorials and write-ups.</PageTitle>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Solutions and problem breakdowns from club rounds, written by the members who
          solved them.
        </p>
      </Section>

      <Section className="pb-22">
        <InDevelopment
          title="The club blog"
          body="Members will be able to write here, and every post is reviewed before it goes live."
          items={["Contest editorials", "Member posts", "Comments"]}
        />
      </Section>
    </>
  );
}
