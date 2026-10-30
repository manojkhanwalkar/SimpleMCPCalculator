# ScientificCalculatorMCP

A [MCP](https://modelcontextprotocol.io) server and client for square roots and logarithms, both
Spring Boot apps built on [Spring AI's MCP starters](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html).
It is the companion of the basic calculator in the parent directory and is built the same way:
the client launches the server and talks to it over **stdio** (JSON-RPC on the server's stdin/stdout).

```
scientific-calculator-mcp-client ──(MCP over stdin/stdout)──> scientific-calculator-mcp-server
  (spring-ai-starter-mcp-client)                                (spring-ai-starter-mcp-server, @McpTool methods)
```

| Module                              | What it is                                                                 |
|-------------------------------------|----------------------------------------------------------------------------|
| `scientific-calculator-mcp-server/` | Spring Boot MCP server; `ScientificTools` holds the four `@McpTool` methods |
| `scientific-calculator-mcp-client/` | Spring Boot MCP client; starts the server jar and calls its tools from the command line |

Versions: Java 25+, Spring Boot 4.1, Spring AI 2.0.1 (MCP Java SDK 2.0).

## Tools

| Tool    | Arguments           | Returns             | Error result when                   |
|---------|---------------------|---------------------|-------------------------------------|
| `sqrt`  | `x` (number)        | square root of `x`  | `x < 0`                             |
| `ln`    | `x`                 | natural log (base e) of `x` | `x <= 0`                    |
| `log10` | `x`                 | base-10 log of `x`  | `x <= 0`                            |
| `log`   | `x`, `base`         | log of `x` in `base` | `x <= 0`, `base <= 0` or `base == 1` |

Every tool also returns an error rather than `NaN` or `Infinity` when given a non-finite input.
`log` returns an exact whole number for exact powers (`log 1000 10` is `3.0`, not
`2.9999999999999996`). All four tools are annotated read-only, non-destructive and idempotent.

## Prerequisites

- **Java 25 or newer** (`java -version`)
- Nothing else. The Maven wrapper (`./mvnw`) downloads Maven on its first run.

All commands below are run from this `ScientificCalculatorMCP/` directory.

## 1. Build

```bash
./mvnw package
```

This builds both executable jars (and runs the tests):

- `scientific-calculator-mcp-server/target/scientific-calculator-mcp-server.jar`
- `scientific-calculator-mcp-client/target/scientific-calculator-mcp-client.jar`

## 2. Start the server

With stdio there is no port to listen on: the server is started by whoever connects to it, and it
reads MCP requests from stdin and writes responses to stdout. **You don't start it yourself when
using the client** — the client launches it as a subprocess (see step 3).

To start it on its own, for example to try it by hand:

```bash
java -jar scientific-calculator-mcp-server/target/scientific-calculator-mcp-server.jar
```

It then waits for JSON-RPC messages on stdin. Paste these lines one at a time (the server takes a
few seconds to start):

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"manual","version":"1"}}}
{"jsonrpc":"2.0","method":"notifications/initialized"}
{"jsonrpc":"2.0","id":2,"method":"tools/list"}
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"sqrt","arguments":{"x":81}}}
{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"log","arguments":{"x":1024,"base":2}}}
```

Press `Ctrl+D` to stop it. Because stdout is the protocol channel, the server's logs go to a file
instead: `${user.home}/.calculator-mcp/scientific-calculator-mcp-server.log` (override with `--logging.file.name=<path>`).

## 3. Use the client

The client reads the server command from `spring.ai.mcp.client.stdio.connections.scientific` in
`scientific-calculator-mcp-client/src/main/resources/application.yml`, starts the server, performs
the MCP handshake, and calls the tools.

```bash
# Demo: list the server's tools and call each one
java -jar scientific-calculator-mcp-client/target/scientific-calculator-mcp-client.jar

# One-operand tools: <operation> <x>
java -jar scientific-calculator-mcp-client/target/scientific-calculator-mcp-client.jar sqrt 144     # 12.0
java -jar scientific-calculator-mcp-client/target/scientific-calculator-mcp-client.jar ln 10        # 2.302585092994046
java -jar scientific-calculator-mcp-client/target/scientific-calculator-mcp-client.jar log10 1000   # 3.0

# Logarithm in any base: log <x> <base>
java -jar scientific-calculator-mcp-client/target/scientific-calculator-mcp-client.jar log 1024 2   # 10.0

# Errors print "Error: ..." and exit with code 1
java -jar scientific-calculator-mcp-client/target/scientific-calculator-mcp-client.jar sqrt -4      # Error: Square root of a negative number
java -jar scientific-calculator-mcp-client/target/scientific-calculator-mcp-client.jar log 8 1      # Error: Logarithm base must be > 0 and not 1
```

An unknown operation, the wrong number of operands (`sqrt 4 2`, `log 8`) or a non-number
(`sqrt NaN`) prints the usage and the server's operations, and exits with code 1.

Demo output:

```
Tools offered by the server: ln, log, log10, sqrt
sqrt  144     = 12.0
sqrt  2       = 1.4142135623730951
log10 1000    = 3.0
log   1024, 2 = 10.0
ln    10      = 2.302585092994046
sqrt  -4      = Error: Square root of a negative number
ln    0       = Error: Logarithm is only defined for x > 0
```

The client finds the server jar at `scientific-calculator-mcp-server/target/scientific-calculator-mcp-server.jar`,
relative to the current directory. To run it from somewhere else, pass the path:

```bash
java -jar /path/to/scientific-calculator-mcp-client.jar --scientific.server.jar=/path/to/scientific-calculator-mcp-server.jar sqrt 2
```

## Use the server from Claude Code or Claude Desktop

Any MCP host can launch the server the same way the client does. For Claude Code:

```bash
claude mcp add scientific-calculator -- java -jar "$(pwd)/scientific-calculator-mcp-server/target/scientific-calculator-mcp-server.jar"
```

For Claude Desktop, add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "scientific-calculator": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/scientific-calculator-mcp-server.jar"]
    }
  }
}
```

It can be registered alongside the basic `calculator` server; the tool names don't overlap.

## Tests

```bash
./mvnw verify
```

- `ScientificToolsTest` unit-tests every tool, including its input checks and exact powers in `log`.
- `ScientificCalculatorClientEndToEndTest` launches the real server jar over stdio, checks the
  advertised tools and their read-only annotations, and calls every tool through
  `ScientificCalculatorClient`, including error cases. The server module builds first, so its jar
  exists by the time this test runs. When the client module is built on its own and the jar is
  missing, the test is skipped.

## How it works

- **Server**: `spring-ai-starter-mcp-server` with `spring.ai.mcp.server.stdio=true`. Its annotation
  scanner registers every `@McpTool` method on a Spring bean and builds the input schema from
  the `@McpToolParam` parameters. An exception thrown by a tool becomes a result with
  `isError: true`. The banner, console logging and web server are switched off so nothing but
  protocol messages reaches stdout.
- **Client**: `spring-ai-starter-mcp-client` creates one initialized `McpSyncClient` for each
  configured stdio connection. `ScientificCalculatorCli` (an `ApplicationRunner`) wraps it in
  `ScientificCalculatorClient`, which sends `tools/call` requests, and then exits, which closes
  the connection and stops the server process.
