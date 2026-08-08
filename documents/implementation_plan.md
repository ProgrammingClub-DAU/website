# Competitive Programming Club Website Implementation Plan

This document outlines a phased approach to building a comprehensive, CP-focused club website. The plan is broken down into manageable phases, starting from a foundational minimum viable product (MVP) to advanced real-time features and community building.

## Resolved Decisions

> [!NOTE]
> 1. **Community Feature (Phase 4):** The team has decided to build the community chat **natively** on the website using WebSockets (Spring WebSockets/STOMP on backend).
> 2. **Blog System:** Will be restricted to `ROLE_ADMIN` users in Phase 1. Open contribution may be considered in later phases.
> 3. **Tech Stack (FINALIZED):** Frontend: Next.js 16.3 (React 19). Backend: Spring Boot 4.1.0 (Java 17). Database: PostgreSQL 15+.
> 4. **Hosting (FINALIZED):** Frontend on Vercel. Backend on Render/AWS. Database on managed Postgres (Render/Supabase).

---

## Proposed Phases

### Phase 1: Foundation (MVP) & Information Hub
*Goal: Establish the basic structure, authentication, user base, and club information.*

- **Project Setup & UI Foundation**: 
  - Set up the frontend and backend architecture. 
  - Implement a modern, dynamic, and premium design system (e.g., dark mode, glassmorphism, micro-animations).
- **Static & Informational Pages**: 
  - Home page and About the Club.
  - **Club Achievements & Hall of Fame**: A dedicated section to showcase the club's past accomplishments, ICPC/regional qualifiers, hackathon wins, and notable alumni.
  - Basic "Club Members" directory.
- **Authentication System**: 
  - User registration restricted to or verified by student email IDs.
  - Collection of basic details: Name, Email, Phone Number.
- **Blog Feature**:
  - A CMS (Content Management System) for publishing articles, coding tutorials, and contest editorials.
  - Markdown support for code snippets and math equations (LaTeX).
  - Comment section on blogs to foster discussion.
- **Basic User Profile**: Allow users to view and edit their basic information.

### Phase 2: CP Platform Integrations & Leaderboards
*Goal: Integrate external platforms, spark competition, and recognize top performers.*

- **Handle Management**: Users can link their profiles from external CP platforms (Codeforces, CodeChef, LeetCode, AtCoder).
- **Data Synchronization**: 
  - Implement backend background jobs (cron) to periodically fetch user stats (ratings, problems solved, contest history) using public APIs.
- **Public Profiles**: Enhance user profiles so others can view their aggregated CP statistics.
- **Overall Leaderboard**: 
  - Create dynamic leaderboards based on different metrics (Highest Rating, Most Problems Solved, Overall Aggregated Score).
- **Weekly Winners & Highlights**:
  - Automated weekly snapshots highlighting the top performers.
  - Badges, spotlights, and homepage shoutouts for "Coder of the Week", "Most Problems Solved This Week", and "Biggest Rating Jump".

### Phase 3: Club Events & Contest Sync
*Goal: Manage club activities and track internal contest performance.*

- **Event Management**:
  - Admin dashboard to create, update, and delete upcoming club events/workshops.
  - User functionality to view and register/RSVP for events.
- **Codeforces Contest Sync**:
  - Integration with Codeforces to track specific club-hosted contests (mashups or group contests).
  - Fetch and render localized, club-specific ranklists and scoreboards for these contests directly on the website.
  - Track overall standing across multiple internal club contests (Club Championship Leaderboard).

### Phase 4: Engagement, Gamification, & Community
*Goal: Keep users engaged daily with interactive features and build a strong native community.*

- **Daily Questions (POTD) & Daily Winner**:
  - A system to fetch or manually post a "Problem of the Day" across different difficulty levels.
  - **Daily Challenge Winner**: Automatically track and highlight the first person (or top fastest people) to solve the POTD.
  - Maintain a streak/point system for daily participation.
- **1v1 Battles**:
  - Real-time matchmaking or "invite a friend" system using WebSockets.
  - Users enter a virtual room, a random problem of an agreed-upon rating is fetched.
  - Real-time tracking of who solves it first to declare a winner.
- **Discord-Type Community (Native Forums & Chat)**:
  - **Channels**: Topic-specific real-time chat rooms (e.g., `#general`, `#web-dev`, `#codeforces-help`, `#cp-resources`).
  - **Threads**: Ability to create discussion threads for specific algorithms or problems.
  - Direct messaging between club members.
  - Presence indicators (Online/Offline/In a Contest).

---

## Verification Plan

### Automated Tests
- Unit testing for API integrations (especially ensuring the Codeforces/LeetCode data fetchers handle rate limits and errors gracefully).
- Unit testing for the User Authentication flow and Blog CRUD operations.

### Manual Verification
- Deploying a staging version of the app to test email delivery for registration.
- Verifying that CP handles sync correctly and the Weekly/Daily winner algorithms calculate correctly.
- Testing the 1v1 battle feature and Community Chat using multiple browser sessions to ensure real-time WebSocket communication is robust and scalable.
