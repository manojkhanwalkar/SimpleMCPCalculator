package com.example.calculator.router;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * Command line front end:
 * <pre>
 *   java -jar calculator-router-client.jar                 # list every operation with its server, then run a demo
 *   java -jar calculator-router-client.jar add 7 5         # routed to calculator-mcp-server
 *   java -jar calculator-router-client.jar log 1024 2      # routed to scientific-calculator-mcp-server
 * </pre>
 * Spring {@code --name=value} options (e.g. {@code --calculator.server.jar=...},
 * {@code --scientific.server.jar=...}) may be mixed in. Exits with 1 on bad usage or when the
 * server reports an error.
 */
@Component
public class CalculatorRouterCli implements ApplicationRunner, ExitCodeGenerator {

    private final CalculatorRouter router;
    private int exitCode;

    public CalculatorRouterCli(List<McpSyncClient> mcpClients) {
        this.router = new CalculatorRouter(mcpClients);
    }

    @Override
    public void run(ApplicationArguments arguments) {
        List<String> args = arguments.getNonOptionArgs();
        if (args.isEmpty()) {
            runDemo();
            return;
        }
        List<String> operands = args.subList(1, args.size());
        if (!operands.stream().allMatch(CalculatorRouterCli::isFiniteNumber)) {
            System.err.println("Usage: java -jar calculator-router-client.jar [<operation> <operand>...]");
            printOperations(System.err);
            exitCode = 1;
            return;
        }
        String result = router.calculate(args.get(0), operands.stream().map(Double::parseDouble).toList());
        System.out.println(result);
        exitCode = result.startsWith("Error:") ? 1 : 0;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private void printOperations(PrintStream out) {
        out.println("Operations:");
        for (Map.Entry<String, CalculatorRouter.Route> entry : router.routes().entrySet()) {
            CalculatorRouter.Route route = entry.getValue();
            out.printf("  %-10s %-12s -> %s%n", entry.getKey(), String.join(", ", route.parameters()), route.server());
        }
    }

    private void runDemo() {
        printOperations(System.out);
        System.out.println();
        demo("add", 17.0, 5.0);
        demo("factorial", 5.0);
        demo("parity", 17.0);
        demo("sqrt", 144.0);
        demo("log", 1024.0, 2.0);
        demo("divide", 1.0, 0.0);
        demo("cube_root", 8.0);
    }

    private void demo(String operation, Double... operands) {
        System.out.printf("%-10s %-13s = %s%n", operation, List.of(operands), router.calculate(operation, List.of(operands)));
    }

    /** NaN and Infinity parse as doubles but can't be sent as JSON numbers, so they are rejected here. */
    private static boolean isFiniteNumber(String s) {
        try {
            return Double.isFinite(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
