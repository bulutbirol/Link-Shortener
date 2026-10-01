# Shortlink

A small URL shortener built with Java and React. People can create an account, make short links, see click totals, and turn links off.

[Live demo](https://shortlink-web-eight.vercel.app)

The Spring Boot API manages accounts and links. A Cloudflare Worker handles redirects, so opening a short link doesn't have to wait for the Java service to wake up. Both use the same PostgreSQL database.

## Run locally

You'll need Java 21, Maven, and Node.js.

```text
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

cd frontend
npm install
npm run dev
```

The local profile uses an in-memory H2 database. The redirect Worker needs a Neon PostgreSQL database, so local dashboard links won't redirect until that is connected.

## Deploy

- API: Docker on Render. Set `DB_URL`, `DB_USER`, `DB_PASSWORD`, `TOKEN_SECRET`, `FRONTEND_ORIGIN`, and `SHORT_BASE_URL`.
- Frontend: Vercel. Set `VITE_API_URL` to the API origin and build `frontend` with `npm run build`.
- Redirects: Cloudflare Workers. Add `DATABASE_URL` as a secret. Set `FRONTEND_URL` if the short domain's homepage should open the app.

The short domain is still undecided, so this repo doesn't contain one. The API and Worker should use a new database dedicated to this project.
