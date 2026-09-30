export function createHandler(db, frontendUrl, allowRequest = async () => true) {
  return async function handle(request, context) {
    const url = new URL(request.url);
    if (url.pathname === '/') {
      return frontendUrl ? Response.redirect(frontendUrl, 302) : new Response('Shortlink is running.', { status: 200 });
    }
    if (!['GET', 'HEAD'].includes(request.method)) {
      return new Response('Method not allowed', { status: 405 });
    }
    const code = url.pathname.slice(1);
    if (!/^[a-zA-Z0-9]{7}$/.test(code)) {
      return new Response('Link not found', { status: 404 });
    }
    if (!(await allowRequest(request))) {
      return new Response('Too many requests', { status: 429, headers: { 'retry-after': '60' } });
    }
    const link = await db.find(code);
    if (!link?.active) {
      return new Response('Link not found', { status: 404 });
    }
    if (request.method === 'GET') context.waitUntil(db.count(code));
    return new Response(null, {
      status: 302,
      headers: {
        location: link.targetUrl,
        'cache-control': 'no-store',
        'referrer-policy': 'no-referrer',
      },
    });
  };
}
