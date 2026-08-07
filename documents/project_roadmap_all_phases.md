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

---

## 🔴 PHASE 4: Engagement, Gamification, & Community
**Objective:** Build highly interactive, real-time features to keep users checking the website every single day.

*   **Daily Questions (POTD) & Streaks:**
    *   An automated system that posts a "Problem of the Day" across different difficulty levels (fetched from LeetCode or manually posted).
    *   **Daily Challenge Winner:** Automatically highlights the first person (or fastest solver) of the POTD.
    *   Users earn points and maintain daily login/solve streaks.
*   **Real-Time 1v1 Battles:**
    *   A competitive arena powered by WebSockets (e.g., Spring WebSockets/STOMP).
    *   Users can invite a friend to a virtual room. The system fetches a random problem of an agreed-upon rating.
    *   Real-time tracking of who solves it first to declare a winner, updating win/loss records on their profiles.
*   **Native Discord-Type Community:**
    *   **Channels:** Topic-specific real-time chat rooms built natively into the website (e.g., `#general`, `#web-dev`, `#codeforces-help`).
    *   **Threads:** Forum-style ability to create discussion threads for specific algorithms or tough problems.
    *   **Presence Indicators:** See who is currently Online, Offline, or "In a Contest".
