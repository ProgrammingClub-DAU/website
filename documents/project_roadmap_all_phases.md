# 🚀 CP Club Website: Master Project Roadmap (Phases 1-4)

This document serves as the long-term master plan for the Competitive Programming Club platform. It details exactly what features will be built across all four major development phases.

## 📌 Technical Foundation (Constant Across All Phases)
*   **Frontend:** React, Next.js, Tailwind CSS, Zustand, Axios
*   **Backend:** Java, Spring Boot, Spring Security, JWT
*   **Database:** PostgreSQL (Flyway/Liquibase for migrations)
*   **Infrastructure:** Docker, GitHub Actions (CI/CD), Vercel (FE), Render/AWS (BE)

---

## 🟢 PHASE 1: Foundation (MVP) & Information Hub
**Objective:** Launch a stable, secure, and usable platform for members to register, read club content, and view the base Codeforces leaderboard.

*   **Project Setup:** Complete initialization of the monorepo, Dockerized PostgreSQL database, and CI/CD pipelines.
*   **Authentication & Security:**
    *   Secure user registration and login using Spring Security and stateless JWTs.
    *   Password hashing via BCrypt.
*   **User Profiles & Directory:**
    *   Dashboard for users to manage their details and link their CP handles.
    *   A public "Club Members" directory displaying all registered students.
*   **Static & Informational Content:**
    *   Premium, dark-mode Home and About Us pages.
    *   **Hall of Fame:** Showcasing the club's past achievements (ICPC qualifiers, hackathons).
*   **Content Management (Blogs):**
    *   A Markdown-supported CMS for admins/authorized users to post coding tutorials and contest editorials.
*   **Basic Leaderboard:**
    *   A Spring Boot scheduled cron job that fetches live Codeforces ratings.
    *   A frontend Leaderboard page ranking members by current CF rating.

---

## 🟡 PHASE 2: Comprehensive Platform Integrations
**Objective:** Expand the platform beyond just Codeforces, pulling in data from all major platforms to spark fierce competition.

*   **Multi-Platform Synchronization:**
    *   Extend backend cron jobs to fetch data from **LeetCode**, **CodeChef**, and **AtCoder**. (This may require web-scraping if public APIs are unavailable).
    *   > **Decided against for CodeChef and AtCoder (Phase 3, D9).** LeetCode is synced. CodeChef has no public API, so its stats would mean scraping profile pages that break on any markup change; both platforms stay **link only** on member profiles. Their public contest *schedules* still appear on the club calendar -- that is not member data.
*   **Advanced Leaderboards:**
    *   Filter and sort leaderboards by specific platforms (e.g., "Top Leetcoders", "Top CodeChef").
    *   Create an "Overall Aggregated Score" combining metrics across all platforms.
*   **Weekly Winners & Highlights:**
    *   Automated weekly snapshots that calculate the best performers of the past 7 days.
    *   Homepage shoutouts and digital badges for: *"Coder of the Week"*, *"Most Problems Solved This Week"*, and *"Biggest Rating Jump"*.
*   **Enhanced Public Profiles:**
    *   Profile pages will now display beautiful charts and graphs showing a user's rating progression over time.

---

## 🟠 PHASE 3: Club Events & Contest Sync
**Objective:** Transition the website into an operational hub for managing physical club activities and internal competitions.

*   **Event Management System:**
    *   Admin dashboard to create, update, and schedule upcoming club events, bootcamps, and workshops.
    *   Users can RSVP/Register for events, and events will show up on a central Club Calendar.
*   **Codeforces Contest Sync (Internal Tracking):**
    *   Integration with Codeforces to track specific club-hosted contests (CF Mashups or Group contests).
    *   The website will automatically fetch and render localized ranklists *just* for those specific contests.
*   **The Club Championship:**
    *   A specialized leaderboard that tracks overall standing across a "season" of multiple internal club contests.

> **Status.** The Phase 3 backend is built: the sync pipeline, stats queries,
> scores and weekly awards, and the calendar are merged; club contests and the
> Championship, the blog workflow and comments, and RSVPs with reminder emails are
> in review. The Phase 3 frontend pages and the production rollout have not
> started. The plan is [phase_3_execution_playbook.md](phase_3_execution_playbook.md).

---

## 🔴 PHASE 4: Engagement, Gamification, & Community
**Objective:** Build highly interactive, real-time features to keep users checking the website every single day.

*   **Daily Questions (POTD) & Streaks:**
    *   An automated system that posts a "Problem of the Day" across different difficulty levels (fetched from LeetCode or manually posted).
    *   **Daily Challenge Winner:** Automatically highlights the first person (or fastest solver) of the POTD.
    *   Users earn points and maintain daily login/solve streaks.
*   **Real-Time 1v1 Battles (Lockout Duels):**
    *   **Architecture:** A competitive arena powered by WebSockets (Spring WebSockets/STOMP) for real-time lobby state sync.
    *   **Logic:** Users can invite a friend to a virtual room. The backend leverages Phase 3's `cf_problems` and `cf_solves` tables to instantly find a random problem matching the desired rating that *neither* user has solved yet.
    *   **Execution:** The timer starts. Since `CodeforcesSubmissionSyncService` paginates natively, a dedicated fast-polling background job checks Codeforces for the users' specific handles to detect the first `Accepted` verdict.
    *   **Rewards:** Declares a winner, distributes "Duel Rating Points" (an Elo system), and awards profile badges (e.g., "Duel Master").
*   **CP Bingo:**
    *   **Architecture:** A 5x5 Grid generator where each square represents a specific algorithmic topic or problem rating criteria.
    *   **Logic:** Heavily relies on the Phase 3 `cf_problems`, `cf_problem_tags`, and `cf_solves` local database tables. This allows the backend to instantly compute and color-code bingo squares based on the user's local sync history without hammering the Codeforces API with hundreds of requests.
    *   **Execution:** Daily, Weekly, and Monthly Bingo cards. Users race to get 5-in-a-row (horizontal, vertical, or diagonal) to win Gamification Points.
*   **Native Discord-Type Community:**
    *   **Channels:** Topic-specific real-time chat rooms built natively into the website (e.g., `#general`, `#web-dev`, `#codeforces-help`).
    *   **Threads:** Forum-style ability to create discussion threads for specific algorithms or tough problems.
    *   **Presence Indicators:** See who is currently Online, Offline, or "In a Contest".

---

## 🧹 Phase 4 cleanup, carried over from Phase 3

Small debts Phase 3 knowingly left, and ones found while building it. None blocks
Phase 3; each is cheaper to clear before Phase 4 builds on top of it.

**Planned in the Phase 3 playbook (Section 10.5)**
*   Drop `blog_posts.published`. V20 kept it so the previous backend release reads correct values during a rolling deploy; once nothing reads it, it can go.
*   Delete the `frontend/src/app/next-api/cf/*` routes if nothing uses them once rating history comes from the API.
*   Add ShedLock before ever running a second backend instance. `@Scheduled` has no distributed lock, and the comment and resend rate limits live in memory (D30).

**Found while building Phase 3**
*   Remove the stray files at the repository root -- `DbCheck.class`, `DbCheck.java`, `TestDto.java`, `TestLombok.java`, `pr_body.txt`, `pr_diff.patch`. A compiled class and a diff dump on the public default branch.
*   Enable `@EnableAsync`, or drop `@Async` from `MailService.send`. Without it the annotation does nothing and every email is sent on the calling thread -- including the verification email, inside the request that triggered it.
*   Decide what to do with email verification. Amendment A2 withdrew it, but Stage 1C built it anyway; it is switched off and harmless, and either the flag gets turned on deliberately or the code is removed.
*   `SyncRunRecorder` stamps runs with `LocalDateTime.now()` -- the JVM's zone -- while `SyncHealthService` compares against UTC (D31). Identical on Render, five and a half hours apart on an IST laptop.
*   `SyncJobDispatcher` writes a `sync_runs` row for every manual trigger that is never finished, beside the row the job itself records.
*   `CodeforcesApiClient` builds its own `RestTemplate` with no timeouts. The configured bean has them; a hung Codeforces call would hold a sync thread indefinitely.
*   Move `events.event_date` to UTC like every Phase 3 column. It stores the IST wall-clock time an admin typed, so every consumer has to know to convert it -- the calendar feed did not, and put club events five and a half hours late until Stage 7 fixed it.
