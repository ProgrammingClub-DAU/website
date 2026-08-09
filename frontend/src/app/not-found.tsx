import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Eyebrow, Section } from "@/components/site/primitives";

export default function NotFound() {
  return (
    <Section className="flex min-h-[60vh] flex-col justify-center py-24">
      <Eyebrow>404</Eyebrow>
      <h1 className="mt-6 max-w-[18ch] text-[clamp(2rem,5vw,3rem)] leading-[1.05] font-[510] tracking-[-0.02em] text-balance">
        That page is not on the board.
      </h1>
      <p className="mt-5 max-w-[46ch] text-base leading-6 text-fg-muted text-pretty">
        The link may be out of date, or the page may not be built yet. Head back to the
        home page, or see what the club runs.
      </p>
      <div className="mt-8 flex flex-wrap gap-3">
        <Button asChild className="h-10 rounded-full px-5.5">
          <Link href="/">Back to home</Link>
        </Button>
        <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
          <Link href="/events">See our events</Link>
        </Button>
      </div>
    </Section>
  );
}
