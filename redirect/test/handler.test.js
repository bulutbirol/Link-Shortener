import test from 'node:test';
import assert from 'node:assert/strict';
import { createHandler } from '../src/handler.js';

test('active short link redirects and counts one click', async () => {
  let clicks = 0;
  const db = {
    find: async () => ({ targetUrl: 'https://example.com/article', active: true }),
    count: async () => { clicks += 1; },
  };
  const pending = [];
  const handler = createHandler(db, 'https://app.example');
  const response = await handler(new Request('https://short.example/Ab123xy'), {
    waitUntil(promise) { pending.push(promise); },
  });
  await Promise.all(pending);
  assert.equal(response.status, 302);
  assert.equal(response.headers.get('location'), 'https://example.com/article');
  assert.equal(response.headers.get('cache-control'), 'no-store');
  assert.equal(clicks, 1);
});

test('disabled and unknown links do not redirect', async () => {
  const handler = createHandler({ find: async () => ({ active: false }) }, 'https://app.example');
  const response = await handler(new Request('https://short.example/Ab123xy'), { waitUntil() {} });
  assert.equal(response.status, 404);
  assert.equal(response.headers.get('location'), null);
});

test('root works before a frontend URL is configured', async () => {
  const handler = createHandler({}, undefined);
  const response = await handler(new Request('https://short.example/'), { waitUntil() {} });
  assert.equal(response.status, 200);
});
