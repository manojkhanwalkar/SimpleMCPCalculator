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
        } else if (args.size() == 2 && isNumber(args.get(1))) {
            print(calculator.calculate(args.get(0), Double.parseDouble(args.get(1))));
        } else if (args.size() == 3 && isNumber(args.get(1)) && isNumber(args.get(2))) {
            print(calculator.calculate(args.get(0), Double.parseDouble(args.get(1)), Double.parseDouble(args.get(2))));
        } else {
            System.err.println("Usage: java -jar scientific-calculator-mcp-client.jar [<operation> <x> | log <x> <base>]");
            System.err.println("Operations: " + String.join(", ", calculator.listOperations()));
            exitCode = 1;
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

    private static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
