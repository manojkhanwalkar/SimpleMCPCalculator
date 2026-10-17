package com.example.scientific.client;

import java.util.List;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * Command line front end:
 * <pre>
 *   java -jar scientific-calculator-mcp-client.jar              # list tools, then run a demo of every operation
 *   java -jar scientific-calculator-mcp-client.jar sqrt 144     # call a one-operand tool and print the result
 *   java -jar scientific-calculator-mcp-client.jar log 1024 2   # call log with x and base
 * </pre>
 * Spring {@code --name=value} options (e.g. {@code --scientific.server.jar=...}) may be mixed in.
 * Exits with 1 on bad usage or when the server reports an error.
 */
@Component
public class ScientificCalculatorCli implements ApplicationRunner, ExitCodeGenerator {

    private final ScientificCalculatorClient calculator;
    private int exitCode;

    public ScientificCalculatorCli(List<McpSyncClient> mcpClients) {
        this.calculator = new ScientificCalculatorClient(mcpClients.getFirst());
    }

    @Override
    public void run(ApplicationArguments arguments) {
        List<String> args = arguments.getNonOptionArgs();
        if (args.isEmpty()) {
            runDemo();
            return;
        }
        String operation = args.get(0);
        List<String> operands = args.subList(1, args.size());
        List<String> operations = calculator.listOperations();
        // log takes x and base; every other tool takes x alone
        int expectedOperands = operation.equals("log") ? 2 : 1;
        if (!operations.contains(operation) || operands.size() != expectedOperands
                || !operands.stream().allMatch(ScientificCalculatorCli::isFiniteNumber)) {
            System.err.println("Usage: java -jar scientific-calculator-mcp-client.jar [<operation> <x> | log <x> <base>]");
            System.err.println("Operations: " + String.join(", ", operations));
            exitCode = 1;
        } else if (expectedOperands == 1) {
            print(calculator.calculate(operation, Double.parseDouble(operands.get(0))));
        } else {
            print(calculator.calculate(operation, Double.parseDouble(operands.get(0)), Double.parseDouble(operands.get(1))));
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private void print(String result) {
        System.out.println(result);
        exitCode = result.startsWith("Error:") ? 1 : 0;
    }

    private void runDemo() {
        System.out.println("Tools offered by the server: " + String.join(", ", calculator.listOperations()));
        System.out.printf("%-5s 144     = %s%n", "sqrt", calculator.calculate("sqrt", 144));
        System.out.printf("%-5s 2       = %s%n", "sqrt", calculator.calculate("sqrt", 2));
        System.out.printf("%-5s 1000    = %s%n", "log10", calculator.calculate("log10", 1000));
        System.out.printf("%-5s 1024, 2 = %s%n", "log", calculator.calculate("log", 1024, 2));
        System.out.printf("%-5s 10      = %s%n", "ln", calculator.calculate("ln", 10));
        System.out.printf("%-5s -4      = %s%n", "sqrt", calculator.calculate("sqrt", -4));
        System.out.printf("%-5s 0       = %s%n", "ln", calculator.calculate("ln", 0));
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
