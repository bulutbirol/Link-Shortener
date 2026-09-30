import { afterEach, expect, test, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App.jsx';

afterEach(() => { cleanup(); localStorage.clear(); vi.restoreAllMocks(); });

test('a signed-in user can create and see a short link', async () => {
  const reactErrors = vi.spyOn(console, 'error').mockImplementation(() => {});
  const link = { code: 'Ab123xy', shortUrl: 'https://short.example/Ab123xy', url: 'https://example.com/article', active: true, clicks: 0, createdAt: '2026-10-01T00:00:00Z' };
  vi.stubGlobal('fetch', vi.fn(async (url, options = {}) => {
    if (String(url).endsWith('/api/auth/login')) return new Response(JSON.stringify({ token: 'test-token', email: 'birol@example.com' }), { status: 200 });
    if (String(url).endsWith('/api/links') && options.method === 'POST') return new Response(JSON.stringify(link), { status: 201 });
    if (String(url).endsWith('/api/links')) return new Response(JSON.stringify([]), { status: 200 });
    throw new Error(`Unexpected request: ${url}`);
  }));

  const user = userEvent.setup();
  render(<App />);
  await user.click(screen.getByRole('button', { name: /sign in/i }));
  await user.type(screen.getByLabelText(/email/i), 'birol@example.com');
  await user.type(screen.getByLabelText(/password/i), 'safe-password-123');
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
  await user.type(await screen.findByLabelText(/long url/i), 'https://example.com/article');
  await user.click(screen.getByRole('button', { name: /create link/i }));
  expect(await screen.findByText('https://short.example/Ab123xy')).toBeTruthy();
  expect(reactErrors.mock.calls.filter(([message]) => String(message).includes('uncontrolled input'))).toHaveLength(0);
});
