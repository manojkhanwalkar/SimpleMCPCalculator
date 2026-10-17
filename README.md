# SimpleMCPCalculator

[![CI](https://github.com/manojkhanwalkar/SimpleMCPCalculator/actions/workflows/ci.yml/badge.svg)](https://github.com/manojkhanwalkar/SimpleMCPCalculator/actions/workflows/ci.yml)

A minimal [MCP](https://modelcontextprotocol.io) server and client, both Spring Boot apps built
on [Spring AI's MCP starters](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html).
The server exposes a calculator as MCP tools; the client launches the server and talks to it over
**stdio** (JSON-RPC on the server's stdin/stdout).

```
calculator-mcp-client ──(MCP over stdin/stdout)──> calculator-mcp-server
  (spring-ai-starter-mcp-client)                     (spring-ai-starter-mcp-server, @McpTool methods)
```

| Module                   | What it is                                                                  |
|--------------------------|-----------------------------------------------------------------------------|
| `calculator-mcp-server/` | Spring Boot MCP server; `CalculatorTools` holds the nine `@McpTool` methods |
| `calculator-mcp-client/` | Spring Boot MCP client; starts the server jar and calls its tools from the command line |
| [`ScientificCalculatorMCP/`](ScientificCalculatorMCP/README.md) | Separate server + client pair for square roots and logarithms (`sqrt`, `ln`, `log10`, `log`); built by the root `./mvnw`, or on its own with `./mvnw` in that directory |
| `calculator-router-client/` | Spring Boot MCP client connected to **both** servers; sends each operation to the server that offers it (see [Router client](#router-client)) |

Versions: Java 25+, Spring Boot 4.1, Spring AI 2.0.1 (MCP Java SDK 2.0).

## Tools

| Tool        | Arguments          | Returns                                       |
|-------------|--------------------|-----------------------------------------------|
| `add`       | `a`, `b` (numbers) | `a + b`                                       |
| `subtract`  | `a`, `b`           | `a - b`                                       |
| `multiply`  | `a`, `b`           | `a * b`                                       |
| `divide`    | `a`, `b`           | `a / b`; error result if `b` is 0             |
| `remainder` | `a`, `b`           | `a % b`; error result if `b` is 0             |
| `exponent`  | `a`, `b`           | `a` raised to the power `b`                   |
| `factorial` | `n` (whole number) | `n!`; error result unless `0 <= n <= 20`      |
| `isPrime`   | `n` (whole number) | `true` or `false`                             |
| `parity`    | `n` (whole number) | `even` or `odd`                               |

The one-operand tools take a whole number: a fraction such as `7.5` (or anything beyond 2^53,
where a JSON number can't be exact) comes back as an error result rather than being truncated.
`factorial` stops at 20 because `21!` no longer fits in a 64-bit integer.
Any tool whose result would overflow or be undefined (e.g. `exponent 10 400`, `exponent -8 0.5`)
returns an error result instead of `Infinity` or `NaN`.

## Prerequisites

- **Java 25 or newer** (`java -version`)
- Nothing else. The Maven wrapper (`./mvnw`) downloads Maven on its first run.

All commands below are run from the repository root.

## 1. Build

```bash
./mvnw package
```

This builds every executable jar (and runs the tests):

- `calculator-mcp-server/target/calculator-mcp-server.jar`
- `calculator-mcp-client/target/calculator-mcp-client.jar`
- `ScientificCalculatorMCP/scientific-calculator-mcp-server/target/scientific-calculator-mcp-server.jar`
- `ScientificCalculatorMCP/scientific-calculator-mcp-client/target/scientific-calculator-mcp-client.jar`
- `calculator-router-client/target/calculator-router-client.jar`

## 2. Start the server

With stdio there is no port to listen on: the server is started by whoever connects to it, and it
reads MCP requests from stdin and writes responses to stdout. **You don't start it yourself when
using the client** — the client launches it as a subprocess (see step 3).

To start it on its own, for example to try it by hand:

```bash
java -jar calculator-mcp-server/target/calculator-mcp-server.jar
```

It then waits for JSON-RPC messages on stdin. Paste these lines one at a time (the server takes a
few seconds to start):

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"manual","version":"1"}}}
{"jsonrpc":"2.0","method":"notifications/initialized"}
{"jsonrpc":"2.0","id":2,"method":"tools/list"}
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"add","arguments":{"a":7,"b":5}}}
```

Press `Ctrl+D` to stop it. Because stdout is the protocol channel, the server's logs go to a file
instead: `${java.io.tmpdir}/calculator-mcp-server.log` (usually `/tmp/calculator-mcp-server.log`).

## 3. Use the client

The client reads the server command from `spring.ai.mcp.client.stdio.connections.calculator` in
`calculator-mcp-client/src/main/resources/application.yml`, starts the server, performs the MCP
handshake, and calls the tools.

```bash
# Demo: list the server's tools and call each one
java -jar calculator-mcp-client/target/calculator-mcp-client.jar

# Call one tool: <operation> <a> <b>
java -jar calculator-mcp-client/target/calculator-mcp-client.jar add 7 5          # 12.0
java -jar calculator-mcp-client/target/calculator-mcp-client.jar subtract 7 5     # 2.0
java -jar calculator-mcp-client/target/calculator-mcp-client.jar multiply 6 7     # 42.0
java -jar calculator-mcp-client/target/calculator-mcp-client.jar divide 10 4      # 2.5
java -jar calculator-mcp-client/target/calculator-mcp-client.jar remainder 10 3   # 1.0
java -jar calculator-mcp-client/target/calculator-mcp-client.jar exponent 2 10    # 1024.0
java -jar calculator-mcp-client/target/calculator-mcp-client.jar divide 1 0       # Error: Division by zero (exit code 1)

# Call a one-operand tool: <operation> <n>
java -jar calculator-mcp-client/target/calculator-mcp-client.jar factorial 5      # 120
java -jar calculator-mcp-client/target/calculator-mcp-client.jar isPrime 97       # true
java -jar calculator-mcp-client/target/calculator-mcp-client.jar parity 42        # even
java -jar calculator-mcp-client/target/calculator-mcp-client.jar parity 7.5       # Error: Not a whole number: 7.5 (exit code 1)
```

Demo output:

```
Tools offered by the server: add, divide, exponent, factorial, isPrime, multiply, parity, remainder, subtract
add       17, 5 = 22.0
subtract  17, 5 = 12.0
multiply  17, 5 = 85.0
divide    17, 5 = 3.4
remainder 17, 5 = 2.0
exponent  17, 5 = 1419857.0
divide    1, 0  = Error: Division by zero
factorial 17    = 355687428096000
isPrime   17    = true
parity    17    = odd
factorial 2.5   = Error: Not a whole number: 2.5
```

The client finds the server jar at `calculator-mcp-server/target/calculator-mcp-server.jar`,
relative to the current directory. To run it from somewhere else, pass the path:

```bash
java -jar /path/to/calculator-mcp-client.jar --calculator.server.jar=/path/to/calculator-mcp-server.jar add 7 5
```

## Router client

`calculator-router-client` launches **both** servers (two stdio connections in its
`application.yml`) and calls whichever one offers the requested operation:

```
                          ┌──(stdio)──> calculator-mcp-server             add, subtract, ..., parity
calculator-router-client ─┤
                          └──(stdio)──> scientific-calculator-mcp-server  sqrt, ln, log10, log
```

At startup `CalculatorRouter` asks each server for its tools (`tools/list`) and builds a routing
table from tool name to server, along with each tool's parameter names in order. Nothing about
the tools is hard-coded, so a tool added to either server is routable with no client change. If
both servers ever offer the same tool name, the client refuses to start rather than guess.

```bash
# List every operation with its parameters and server, then run a demo against both servers
java -jar calculator-router-client/target/calculator-router-client.jar

# <operation> <operand>...  (operands fill the tool's parameters in order)
java -jar calculator-router-client/target/calculator-router-client.jar add 7 5        # 12.0  (calculator-mcp-server)
java -jar calculator-router-client/target/calculator-router-client.jar isPrime 97     # true  (calculator-mcp-server)
java -jar calculator-router-client/target/calculator-router-client.jar log 1000 10    # 3.0   (scientific-calculator-mcp-server)

# Errors print "Error: ..." and exit with code 1
java -jar calculator-router-client/target/calculator-router-client.jar log 8          # Error: log takes 2 operand(s): x, base
java -jar calculator-router-client/target/calculator-router-client.jar cube_root 8    # Error: Unknown operation: cube_root
java -jar calculator-router-client/target/calculator-router-client.jar divide 1 0     # Error: Division by zero
```

Both server jars are found relative to the repository root. From elsewhere, pass both paths:

```bash
java -jar /path/to/calculator-router-client.jar \
  --calculator.server.jar=/path/to/calculator-mcp-server.jar \
  --scientific.server.jar=/path/to/scientific-calculator-mcp-server.jar sqrt 2
```

## Use the server from Claude Code or Claude Desktop

Any MCP host can launch the server the same way the client does. For Claude Code:

```bash
claude mcp add calculator -- java -jar "$(pwd)/calculator-mcp-server/target/calculator-mcp-server.jar"
```

For Claude Desktop, add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "calculator": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/calculator-mcp-server.jar"]
    }
  }
}
```

## Tests

```bash
./mvnw verify
```

- `CalculatorToolsTest` unit-tests every tool, including the division-by-zero and whole-number checks.
- `CalculatorClientEndToEndTest` launches the real server jar over stdio and calls every tool
  through `CalculatorClient`. The server module builds first, so its jar exists by the time
  this test runs. When the client module is built on its own and the jar is missing, the test
  is skipped.
- `CalculatorRouterEndToEndTest` launches both server jars and checks that every operation is
  routed to the right server with the right parameter names, calls operations on each server,
  and checks the error cases: unknown operation, wrong operand count, server errors, and the
  same tool name offered by two servers.
- The `ScientificCalculatorMCP` tests are described in [its README](ScientificCalculatorMCP/README.md).

## How it works

- **Server**: `spring-ai-starter-mcp-server` with `spring.ai.mcp.server.stdio=true`. Its annotation
  scanner registers every `@McpTool` method on a Spring bean and builds the input schema from
  the `@McpToolParam` parameters. An exception thrown by a tool becomes a result with
  `isError: true`. The banner, console logging and web server are switched off so nothing but
  protocol messages reaches stdout.
- **Client**: `spring-ai-starter-mcp-client` creates one initialized `McpSyncClient` for each
  configured stdio connection. `CalculatorCli` (an `ApplicationRunner`) wraps it in
  `CalculatorClient`, which sends `tools/call` requests, and then exits, which closes the
  connection and stops the server process.
