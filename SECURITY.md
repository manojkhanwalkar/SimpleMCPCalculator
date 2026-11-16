# Security policy

## Supported versions

Only the latest commit on `master` (and the latest GitHub Release, if any) receives fixes.

## Reporting a vulnerability

Please **do not open a public issue** for a security problem. Report it privately through
GitHub's [private vulnerability reporting](https://github.com/manojkhanwalkar/SimpleMCPCalculator/security/advisories/new)
(Security tab → "Report a vulnerability"), with:

- what is affected (module, file, version or commit),
- how to reproduce it, and
- the impact you expect.

You should get a first answer within 7 days. Once a fix is released the advisory is published
and you are credited unless you ask not to be.

## Scope and threat model

The servers speak MCP over **stdio** only: they open no network port and are started as a
subprocess by the client that uses them, with that user's privileges. The tools are pure
arithmetic functions with no file, network or shell access. Relevant issues are therefore mainly:

- a tool input that crashes or hangs a server, or returns a wrong result as a success,
- anything written to stdout other than MCP messages (it would corrupt the protocol),
- log files readable by other local users (logs go to an owner-only `~/.calculator-mcp/`),
- vulnerable dependencies (Dependabot watches them; dependency review and CodeQL run on every
  pull request).
