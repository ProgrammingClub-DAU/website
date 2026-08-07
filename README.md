# Competitive Programming Club Website

This repository contains the source code for the CP Club Website.

## Project Structure
- `frontend/`: Next.js web application.
- `backend/`: Spring Boot REST API.
- `documents/`: Project planning, architecture, and execution playbooks.

## Local Development

### 1. Database
Run the PostgreSQL database locally using Docker Compose:
```bash
docker-compose up -d
```

### 2. Frontend
```bash
cd frontend
npm install
npm run dev
```

### 3. Backend
```bash
cd backend
./mvnw spring-boot:run
```
