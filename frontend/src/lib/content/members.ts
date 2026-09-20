/**
 * Everyone who built this site, with detailed contributor profiles and roles.
 *
 * Not tied to the committee: contributors stay here after their term ends, and
 * new names are appended as work continues.
 */

export interface ContributorProfile {
  name: string;
  role: string;
  headline: string;
  tags: string[];
  githubUsername?: string;
  badge?: string;
}

export const CONTRIBUTOR_PROFILES: ContributorProfile[] = [
  {
    name: "Madhav Thesiya",
    role: "Core Developer",
    headline: "LeetCode GraphQL synchronization, profile APIs & batch workers",
    tags: ["GraphQL", "Data Sync", "Spring Services", "REST APIs"],
    badge: "Core Contributor",
  },
  {
    name: "Raj Patel",
    role: "Full Stack Engineer",
    headline: "Platform security, JWT auth pipelines & core architecture",
    tags: ["Spring Boot", "JWT Auth", "Next.js", "Security"],
    badge: "System Architect",
  },
  {
    name: "Tanishq Shah",
    role: "Full Stack Developer",
    headline: "Frontend modernizations, dynamic components & UI animations",
    tags: ["Next.js 16", "TypeScript", "Tailwind CSS", "Framer Motion"],
    badge: "Core Contributor",
  },
  {
    name: "Shane Christian",
    role: "Backend Architect",
    headline: "Database migrations, Flyway schemas & core REST services",
    tags: ["Java", "Spring Boot", "PostgreSQL", "Flyway"],
    badge: "Backend Lead",
  },
  {
    name: "Gaurav Rathod",
    role: "Frontend Engineer",
    headline: "Interactive dashboard views, responsive design & site components",
    tags: ["React 19", "Tailwind CSS", "UI/UX", "State Management"],
    badge: "Core Contributor",
  },
  {
    name: "Mahek Kanani",
    role: "Product Lead & Full Stack",
    headline: "System requirements, product roadmap & full-stack integration",
    tags: ["System Design", "Full Stack", "Spring Boot", "Next.js"],
    badge: "Project Lead",
  },
];

/** Backward-compatible list of contributor names. */
export const credits = CONTRIBUTOR_PROFILES.map((c) => c.name);



