import { useEffect, useState } from 'react';

const API = import.meta.env.VITE_API_URL || '';

async function request(path, options = {}, token) {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || (response.status === 429 ? 'Please try again later.' : 'Something went wrong.'));
  return data;
}

function ArrowIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="M5 12h14m-6-6 6 6-6 6" /></svg>;
}

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem('shortlink-token') || '');
  const [email, setEmail] = useState(() => localStorage.getItem('shortlink-email') || '');
  const [view, setView] = useState('home');
  const [links, setLinks] = useState([]);
  const [url, setUrl] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [loadingLinks, setLoadingLinks] = useState(false);

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    setLoadingLinks(true);
    request('/api/links', {}, token)
      .then(data => { if (!cancelled) setLinks(data); })
      .catch(error => { if (!cancelled) setMessage(error.message); })
      .finally(() => { if (!cancelled) setLoadingLinks(false); });
    return () => { cancelled = true; };
  }, [token]);

  async function submitAuth(event) {
    event.preventDefault();
    setBusy(true);
    setMessage('');
    const form = new FormData(event.currentTarget);
    try {
      const data = await request(`/api/auth/${view === 'register' ? 'register' : 'login'}`, {
        method: 'POST',
        body: JSON.stringify({ email: form.get('email'), password: form.get('password') }),
      });
      localStorage.setItem('shortlink-token', data.token);
      localStorage.setItem('shortlink-email', data.email);
      setEmail(data.email);
      setToken(data.token);
      setView('home');
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function createLink(event) {
    event.preventDefault();
    setBusy(true);
    setMessage('');
    try {
      const link = await request('/api/links', { method: 'POST', body: JSON.stringify({ url }) }, token);
      setLinks(previous => [link, ...previous]);
      setUrl('');
      setMessage('Your link is ready.');
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function disableLink(code) {
    setMessage('');
    try {
      const changed = await request(`/api/links/${code}/disable`, { method: 'PATCH' }, token);
      setLinks(previous => previous.map(link => link.code === code ? changed : link));
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function copy(text) {
    try {
      await navigator.clipboard.writeText(text);
      setMessage('Copied to clipboard.');
    } catch {
      setMessage('Copy is unavailable in this browser.');
    }
  }

  function signOut() {
    localStorage.removeItem('shortlink-token');
    localStorage.removeItem('shortlink-email');
    setToken('');
    setEmail('');
    setLinks([]);
    setView('home');
    setMessage('');
  }

  return <div className="site-shell">
    <div className="ambient ambient-one" /><div className="ambient ambient-two" />
    <header className="topbar wrap">
      <a className="brand" href="/" onClick={event => { event.preventDefault(); setView('home'); setMessage(''); }} aria-label="Shortlink home">
        <span className="brand-mark"><span /></span><span>shortlink<span className="brand-dot">.</span></span>
      </a>
      <div className="topbar-right">
        <span className="nav-note">LINKS, MADE SIMPLE</span>
        {token ? <button className="ghost-button" onClick={signOut}>Sign out</button> : view === 'home' ? <button className="ghost-button" onClick={() => setView('login')}>Sign in</button> : <button className="ghost-button" onClick={() => { setView('home'); setMessage(''); }}>Back home</button>}
      </div>
    </header>

    {token ? <main key="dashboard" className="wrap dashboard">
      <div className="dashboard-heading">
        <div><p className="eyebrow"><span className="eyebrow-line" /> YOUR WORKSPACE</p><h1>Your links<span className="accent-dot">.</span></h1><p className="subline">A clear view of every link you share.</p></div>
        <div className="account-pill"><span className="online-dot" />{email}</div>
      </div>
      <section className="glass-card create-card" aria-label="Create a link">
        <div className="section-label"><span>01 / NEW LINK</span><span>{links.length} / 20 LINKS</span></div>
        <h2>Something to share?</h2>
        <p>Paste a long URL. We’ll make it easier to carry around.</p>
        <form className="create-form" onSubmit={createLink}>
          <label className="sr-only" htmlFor="long-url">Long URL</label>
          <input id="long-url" type="url" required placeholder="https://example.com/a/very/long/address" value={url ?? ''} onChange={event => setUrl(event.target.value)} />
          <button className="primary-button" type="submit" disabled={busy || links.length >= 20}>Create link <ArrowIcon /></button>
        </form>
      </section>
      {message && <p className="notice" role="status">{message}</p>}
      <section className="links-section" aria-label="Your links">
        <div className="list-heading"><div><span className="section-index">02 / YOUR COLLECTION</span><h2>All links <span>{links.length.toString().padStart(2, '0')}</span></h2></div><span className="list-caption">SHORT &amp; SWEET</span></div>
        {loadingLinks ? <div className="glass-card empty-state">Loading your links…</div> : links.length === 0 ? <div className="glass-card empty-state"><span className="empty-glyph">↗</span><h3>A fresh start.</h3><p>Your first short link will appear here.</p></div> : <div className="link-list">{links.map(link => <article className={`glass-card link-row ${link.active ? '' : 'is-disabled'}`} key={link.code}>
          <div className="link-main"><a className="short-url" href={link.shortUrl} target="_blank" rel="noreferrer">{link.shortUrl}</a><p title={link.url}>{link.url}</p></div>
          <div className="link-meta"><span className="clicks"><strong>{link.clicks}</strong> clicks</span><span className={`status ${link.active ? 'status-active' : ''}`}>{link.active ? 'Active' : 'Disabled'}</span></div>
          <div className="row-actions"><button onClick={() => copy(link.shortUrl)} aria-label={`Copy ${link.code}`}>Copy</button>{link.active && <button onClick={() => disableLink(link.code)} aria-label={`Disable ${link.code}`}>Disable</button>}</div>
        </article>)}</div>}
      </section>
    </main> : view === 'home' ? <main key="home" className="wrap hero-grid">
      <section className="hero-copy"><p className="eyebrow"><span className="eyebrow-line" /> A LITTLE LINK, A LOT EASIER</p><h1>Good things<br />come in <em>short</em><br />links<span className="accent-dot">.</span></h1><p className="hero-description">Turn the long, messy URLs you share every day into clean little links. Keep them together. Know when they’re clicked.</p><button className="primary-button hero-cta" onClick={() => setView('register')}>Get started for free <ArrowIcon /></button><div className="hero-footnote"><span className="small-rule" />Free to use · Create an account to start</div></section>
      <aside className="hero-art" aria-label="Example of a shorter link"><div className="orbit orbit-large" /><div className="orbit orbit-small" /><div className="floating-card card-long"><span className="mini-label">BEFORE</span><span className="faux-url">example.com/articles/2026/things-to-share...</span></div><div className="connector"><span>↘</span></div><div className="floating-card card-short"><span className="mini-label">AFTER</span><span className="big-short">short.link/<b>Ab123xy</b></span><span className="card-spark">✳</span></div><span className="hero-art-note">LESS TO TYPE. MORE TO SHARE.</span></aside>
      <section className="feature-strip"><div><span className="feature-number">01</span><h3>Make it short</h3><p>One tidy link, ready to go.</p></div><div><span className="feature-number">02</span><h3>Keep track</h3><p>See how many times it’s opened.</p></div><div><span className="feature-number">03</span><h3>Stay in control</h3><p>Turn a link off whenever you need.</p></div></section>
    </main> : <main key="auth" className="auth-layout wrap"><section className="auth-intro"><p className="eyebrow"><span className="eyebrow-line" /> YOUR NEXT LINK STARTS HERE</p><h1>{view === 'register' ? <>Small link.<br /><em>Big hello.</em></> : <>Welcome<br /><em>back.</em></>}</h1><p>Everything you share, in one calm place.</p><div className="auth-decoration"><span>↗</span></div></section><section className="glass-card auth-card"><span className="section-index">{view === 'register' ? '01 / CREATE AN ACCOUNT' : '01 / SIGN IN'}</span><h2>{view === 'register' ? 'Create your account' : 'Sign in'}</h2><p>{view === 'register' ? 'Just an email and password to get started.' : 'Your links are right where you left them.'}</p><form onSubmit={submitAuth}>
      <label htmlFor="email">Email</label><input id="email" name="email" type="email" required autoComplete="email" placeholder="you@example.com" />
      <label htmlFor="password">Password</label><input id="password" name="password" type="password" required minLength={10} autoComplete={view === 'register' ? 'new-password' : 'current-password'} placeholder="At least 10 characters" />
      <button className="primary-button auth-submit" type="submit" disabled={busy}>{busy ? 'One moment…' : view === 'register' ? 'Create account' : 'Sign in'} <ArrowIcon /></button>
    </form>{message && <p className="form-error" role="alert">{message}</p>}<p className="auth-switch">{view === 'register' ? 'Already have an account?' : 'New here?'} <button onClick={() => { setView(view === 'register' ? 'login' : 'register'); setMessage(''); }}>{view === 'register' ? 'Sign in' : 'Create an account'}</button></p></section></main>}
    <footer className="wrap footer"><span>© {new Date().getFullYear()} Shortlink</span><span>SMALL LINKS, CLEAR INTENT.</span></footer>
  </div>;
}
