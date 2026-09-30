import { neon } from '@neondatabase/serverless';
import { createHandler } from './handler.js';

export default {
  async fetch(request, env, context) {
    if (!env.DATABASE_URL && new URL(request.url).pathname !== '/') {
      return new Response('Service unavailable', { status: 503 });
    }
    const sql = env.DATABASE_URL ? neon(env.DATABASE_URL) : null;
    const db = {
      async find(code) {
        const rows = await sql`SELECT target_url, active FROM short_links WHERE code = ${code} LIMIT 1`;
        return rows[0] && { targetUrl: rows[0].target_url, active: rows[0].active };
      },
      async count(code) {
        await sql`UPDATE short_links SET click_count = click_count + 1 WHERE code = ${code} AND active = true`;
      },
    };
    return createHandler(db, env.FRONTEND_URL)(request, context);
  },
};
