# Security Policy — IronAPP

- Tokens are stored only in private app DataStore, never logged.
- Report vulnerabilities via GitHub Security Advisories on this repo.
- The panel is the source of truth for quota/expiry; client checks are UX-only
  and the server enforces 403 independently.
