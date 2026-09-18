import type { Metadata } from "next";
import Link from "next/link";

import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: "Privacy policy",
  description: `What the ${site.fullName} website collects, what is shown publicly, and how to have it removed.`,
};

/** Change this whenever the policy's substance changes. */
const LAST_UPDATED = "18 September 2026";

/**
 * The site's privacy policy.
 *
 * Written from what the code actually stores and shows -- the User entity, the
 * phone visibility rule in PublicUserResponseDto, the attendance table, the
 * Sheets export in lib/google-sheets.ts -- not from a template. If any of those
 * change, this page has to change with them; a policy that describes a different
 * site is worse than none. Google's OAuth consent screen links here.
 */
export default function PrivacyPage() {
  return (
    <>
      <Section className="pt-10 pb-8 md:pt-14">
        <Eyebrow>Privacy</Eyebrow>
        <PageTitle className="max-w-[20ch]">Privacy policy.</PageTitle>
        <p className="mt-6 max-w-[60ch] text-base leading-6 text-fg-muted text-pretty">
          This website is run by the {site.fullName}, a student club at {site.university}. This
          page explains what the site stores about you, who can see it, and how to have it
          removed.
        </p>
        <p className="mt-3 font-mono text-label tracking-caps text-fg-subtle uppercase">
          Last updated {LAST_UPDATED}
        </p>
      </Section>

      <Section className="pb-22">
        <div className="max-w-[68ch] space-y-10 text-base leading-7 text-fg-muted [&_h2]:font-heading [&_h2]:text-xl [&_h2]:font-medium [&_h2]:tracking-[-0.01em] [&_h2]:text-foreground [&_li]:pl-1 [&_strong]:text-foreground [&_ul]:mt-3 [&_ul]:list-disc [&_ul]:space-y-2 [&_ul]:pl-5">
          <div>
            <h2>What we collect</h2>
            <p className="mt-3">
              <strong>When you sign in with Google</strong>, Google tells us your name, your
              university email address and the address of your Google profile picture. Only
              @dau.ac.in accounts can sign in.
            </p>
            <p className="mt-3">
              <strong>What you add to your profile:</strong>
            </p>
            <ul>
              <li>your phone number</li>
              <li>your year of study</li>
              <li>your Codeforces and LeetCode handles</li>
              <li>links to your CodeChef, AtCoder, GitHub and LinkedIn profiles</li>
            </ul>
            <p className="mt-3">
              <strong>From Codeforces and LeetCode</strong>, using the handles you added: your
              public rating and contest history. This is information those sites already publish.
            </p>
            <p className="mt-3">
              <strong>What club admins record:</strong>
            </p>
            <ul>
              <li>your club role, if you hold one</li>
              <li>your batch, for batch photos</li>
              <li>which events you attended, and results such as winners</li>
              <li>photos from events and achievements, which may show you</li>
            </ul>
          </div>

          <div>
            <h2>What is public</h2>
            <p className="mt-3">
              Anyone visiting the site can see your name, profile picture, year, club role,
              Codeforces and LeetCode handles and ratings, and the profile links you added. They
              appear on your profile page and on the leaderboard.
            </p>
            <ul>
              <li>
                <strong>Your email address</strong> is never shown to other visitors.
              </li>
              <li>
                <strong>Your phone number</strong> is shown publicly only if you hold a club post
                (Convenor down to Batch Representative), so that people can reach the club. For
                everyone else it is visible only to club admins, so they can contact you about an
                event.
              </li>
            </ul>
          </div>

          <div>
            <h2>Google Drive access, for admins only</h2>
            <p className="mt-3">
              Club admins can export an event&apos;s attendance to Google Sheets. When they do,
              Google asks them to allow access to &quot;only the specific Google Drive files you
              use with this app&quot; (the <code className="font-mono text-sm">drive.file</code>{" "}
              permission). The site uses this for one thing: creating the spreadsheet the admin
              asked for, in that admin&apos;s own Drive.
            </p>
            <ul>
              <li>It cannot see or open any other file in the admin&apos;s Drive.</li>
              <li>
                The access token stays in the admin&apos;s browser. It is never sent to our server
                or stored.
              </li>
              <li>
                Access can be removed at any time at{" "}
                <a
                  href="https://myaccount.google.com/permissions"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-primary underline-offset-4 hover:underline"
                >
                  myaccount.google.com/permissions
                </a>
                .
              </li>
            </ul>
            <p className="mt-3">
              Our use of information received from Google APIs follows the{" "}
              <a
                href="https://developers.google.com/terms/api-services-user-data-policy"
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary underline-offset-4 hover:underline"
              >
                Google API Services User Data Policy
              </a>
              , including the Limited Use requirements.
            </p>
          </div>

          <div>
            <h2>The live attendance sheet</h2>
            <p className="mt-3">
              The club may keep a Google Sheet of event attendance that updates by itself. When
              it is set up, our server writes each event&apos;s attendance list to it a few
              seconds after an admin changes the list. It writes the same six columns as the
              attendance export: name, Codeforces profile link, email, student ID, year, and the
              time you were marked present. Phone numbers are not included.
            </p>
            <p className="mt-3">
              The server writes as a Google service account, which can only open spreadsheets
              the club has shared with it. The sheet itself is managed by the club&apos;s admins.
            </p>
          </div>

          <div>
            <h2>What we do not do</h2>
            <ul>
              <li>We do not sell or share your information with anyone.</li>
              <li>We do not show advertising.</li>
              <li>We do not use analytics or tracking cookies.</li>
            </ul>
          </div>

          <div>
            <h2>Where it is kept</h2>
            <p className="mt-3">
              The website is hosted on Vercel, the server on Render, the database with a managed
              PostgreSQL provider, and photos on Cloudinary. They store the data on our behalf.
            </p>
            <p className="mt-3">
              Your browser keeps your sign-in session and your light or dark theme choice in local
              storage, so you stay signed in between visits. Signing out removes the session.
            </p>
          </div>

          <div>
            <h2>Removing your information</h2>
            <p className="mt-3">
              You can change or clear anything you added to your profile at any time. To have
              your account deleted, ask any member of the committee listed on the{" "}
              <Link href="/members" className="text-primary underline-offset-4 hover:underline">
                Members page
              </Link>
              . Deleting your account removes your profile and everything on it from the site.
            </p>
          </div>

          <div>
            <h2>Changes</h2>
            <p className="mt-3">
              If this policy changes, this page will be updated and the date at the top will
              change.
            </p>
          </div>
        </div>
      </Section>
    </>
  );
}
