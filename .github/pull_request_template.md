## What and why

<!-- What does this change, and why? Link the issue if there is one. -->

## Testing

- [ ] `./mvnw -B -ntp verify` passes locally (unit tests, end-to-end tests, coverage gate)
- [ ] Each behaviour change has a test that fails without it
- Coverage (from the CI job summary): <!-- e.g. lines 99.3%, branches 96.4%; say what is uncovered and why if below 95% -->

## Review checklist

- [ ] Independent review (`pr-review`) done; findings table posted on this PR
- [ ] `security-review` run on the diff
- [ ] `owasp-llm-top10 --diff` run if MCP / tool / agent code changed
- [ ] README and design doc updated if behaviour, tools, APIs or architecture changed
- [ ] If tool results, schemas, error texts or CLI output changed: noted for golden re-capture in the Python port (SimpleMCPCalculatorPy)
