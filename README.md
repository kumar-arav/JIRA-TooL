# IntelliSprint — AI-Powered Agile Project Management Platform

An enterprise-grade Jira-style Project Management & Ticketing System with **Java Spring Boot 3** backend and **React 18 + TypeScript** frontend.

## ✨ Features

- **9 User Roles** with RBAC: Admin, Scrum Master, Project Owner, CTO, VP, Manager, Developer, Tester, Trainee
- **JWT Authentication + MFA flow** (Spring Security, BCrypt, refresh tokens)
- **Projects & Sprints** — create, start, complete, timeline view
- **AI Task Generation** — describe a project, get sprint tasks with story points, priority, and suggested roles (Groq-api)
- **Kanban Board** — drag-and-drop status changes (TODO → In Progress → In Review → Testing → Closed)
- **Ticket Management** — assigner/assignee, comments, story points, due dates
- **Ticket Closure Workflow** — requires closure notes + tester approval + manager approval
- **Resource Allocation** — team workload & utilization tracking
- **Reports** — burndown, velocity, priority/status charts (Recharts)
- **Notifications** — in-app with unread counts
- **Swagger / OpenAPI docs** built in

## 🧰 Tech Stack

| Layer     | Tech |
|-----------|------|
| Frontend  | React 18, TypeScript, Vite, Tailwind CSS, Redux Toolkit, React Router 6, Recharts, Axios, Lucide |
| Backend   | Java 17, Spring Boot 3.2, Spring Security (JWT), Spring Data JPA, Lombok, SpringDoc OpenAPI |
| Database  | H2 in-memory (dev) — MySQL/PostgreSQL ready (uncomment driver in `pom.xml`) |
| AI        | Mock mode included,Groq(api)based task generations |

## 🚀 Run

### Backend (port 8080)
```bash
cd backend
mvn spring-boot:run
```
- API base: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- H2 console: `http://localhost:8080/api/h2-console` (JDBC URL `jdbc:h2:mem:flowsyncdb`, user `sa`, password `password`)

### Frontend (port 3000)
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:3000` — API requests are proxied to the backend.

## 🔑 Demo Accounts (password for all: `password123`)

| Role | Email |
|------|-------|
| Admin | admin@flowsync.com |
| Scrum Master | sarah.chen@flowsync.com |
| Project Owner | olivia.grant@flowsync.com |
| CTO | kevin.wu@flowsync.com |
| VP | victor.pace@flowsync.com |
| Manager | rita.patel@flowsync.com |
| Developer | james.doe@flowsync.com |
| Tester | priya.rao@flowsync.com |
| Trainee | dan.okafor@flowsync.com |

The login page has role quick-select buttons that autofill credentials.

## 📁 Structure

```
flowsync-app/
├── backend/               Spring Boot API
│   └── src/main/java/com/flowsync/
│       ├── entity/        User, Project, Sprint, Ticket, Comment, Notification…
│       ├── repository/    Spring Data JPA repos
│       ├── service/impl/  Auth, Project, Sprint, Ticket, AI services
│       ├── controller/    REST controllers (auth, projects, sprints, tickets, ai, users, notifications)
│       ├── security/      JwtUtil, JwtAuthFilter
│       └── config/        SecurityConfig (CORS/RBAC), DataSeeder (demo data)
└── frontend/              React + Vite
    └── src/
        ├── api/           Axios clients per resource
        ├── store/         Redux Toolkit (auth persisted to localStorage)
        ├── pages/         login, dashboard, projects, sprints, kanban, tickets, ai, resources, reports, notifications
        └── components/    AppLayout (sidebar + header)
```

## 🔒 Ticket Closure Rules
A ticket can only move to **CLOSED** when:
1. Tester approved ✅
2. Manager approved ✅
3. Closure notes provided ✍️

The backend enforces this in `TicketServiceImpl.updateStatus`.

## 🎛️ Production Notes
- Switch DB: uncomment MySQL dependency in `pom.xml`, update `application.yml` datasource, set `ddl-auto: update`.
- Real AI: set `app.ai.mock: false` and provide `AZURE_OPENAI_ENDPOINT` / `AZURE_OPENAI_KEY` env vars, then implement the HTTP call in `AIServiceImpl`.
- Set a strong `JWT_SECRET` env var.

## 🎛️ Role-Specific Dashboards (v2)

Every role now gets its **own dashboard component** with role-appropriate KPIs, sections, and quick actions. `DashboardPage.tsx` routes by `user.role`:

| Role | Component | Key content |
|------|-----------|-------------|
| Admin | `AdminDashboard` | Users, active users, system health · user management table, RBAC permission matrix, login activity, audit logs, security report |
| Scrum Master | `ScrumMasterDashboard` | 6 KPIs (projects, sprints, tickets, velocity) · sprint progress, burndown, team utilization, timeline, activities, high-priority + blocked tickets, velocity & allocation reports |
| Project Owner | `ProjectOwnerDashboard` | Completion %, sprints, tickets, budget · project health, milestones, deliverables, risks, sprint performance |
| CTO | `CTODashboard` | Projects, on-time %, utilization, risks · portfolio overview, health matrix, status distribution, team capacity, delivery trend |
| VP | `VPDashboard` | Portfolio health, delivery success · executive summary, portfolio view, org utilization, high-risk projects, dept performance, quarterly metrics |
| Manager | `ManagerDashboard` | Team, projects, sprint %, delayed · **approval queue (live approve buttons)**, risks, performance, utilization, workload chart, reports |
| Developer | `DeveloperDashboard` | Assigned/completed/pending/overdue (from `GET /tickets/my`) · active tasks, deadlines, recent assignments, status donut, progress chart, quick actions |
| Tester | `TesterDashboard` | Pending testing, passed, failed, defects · **pending review queue (live approve buttons)**, failed cases, reopened, defect trend, quick actions |
| Trainee | `TraineeDashboard` | Tasks + learning assignments · my tasks, deadlines, mentor feedback, submit/upload actions |

**RBAC is enforced in three layers:**
1. **Sidebar** (`AppLayout.tsx`) — nav items declare `roles: [...]`; Trainees don't see AI Planner/Reports, only leadership sees Sprints/Resources/Reports.
2. **Header actions** — "AI Generate" and "New Ticket" buttons only render for permitted roles.
3. **Backend `@PreAuthorize`** — sprint create/start/complete (Admin/SM), ticket create (Admin/SM/PO/Manager), tester approval (Tester/Admin), manager approval (Manager/Admin), AI generation (Admin/SM/PO).

Shared plumbing lives in `src/components/dashboard/`:
- `useDashboardData.ts` — single hook fetching projects, sprints, tickets, users, my-tickets, notifications once; every role dashboard derives its own view from it.
- `shared.tsx` — `KpiCard`, `Section`, `ProgressRow`, `EmptyRow` primitives reused by all 9 dashboards.
