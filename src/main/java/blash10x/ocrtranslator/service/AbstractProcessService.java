package blash10x.ocrtranslator.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Author: myungsik.sung@gmail.com
 */
public abstract class AbstractProcessService {
  private static final ExecutorService executorService = Executors.newCachedThreadPool();
  protected final ConfigLoader configLoader;
  private final String processName;
  private Process process;
  private BufferedWriter writer;

  protected AbstractProcessService(String processName) {
    configLoader = ConfigLoader.getConfigLoader();
    this.processName = processName;
  }

  protected void start(String command) {
    start(command, null);
  }

  protected void start(String command, Consumer<String> outputConsumer) {
    try {
      ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);

      //builder.redirectInput(ProcessBuilder.Redirect.PIPE); // Default: Redirect.PIPE
      //builder.redirectOutput(ProcessBuilder.Redirect.PIPE); // Default: Redirect.PIPE
      builder.redirectErrorStream(true);

      process = builder.start();
      executorService.execute(new OutputHandler(outputConsumer));
      writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream())); // get stream for input to the process
      System.out.printf("%s has started: %s%n", processName, command);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        executorService.close();

        if (!process.isAlive()) {
          System.out.printf("%s has already been terminated.%n", processName);
          return;
        }
        close();
      }));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void writeToProcess(String str) {
    try {
      writer.write(str);
      writer.flush(); // Important: Deadlock Prevention
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void close() {
    executorService.close();

    writeToProcess("q\n"); // termination command: 'q'
    try {
      writer.close();

      int exitCode = process.waitFor();
      System.out.printf("%s has closed: exitCode=%d%n", processName, exitCode);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      process.descendants().forEach(ProcessHandle::destroy);
      process.destroy();
    }
  }

  private class OutputHandler implements Runnable {
    private final Consumer<String> consumer;

    public OutputHandler(Consumer<String> consumer) {
      this.consumer = consumer;
    }

    public void run() {
      InputStream pis = process.getInputStream();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(pis, StandardCharsets.UTF_8))) {
        for (String line; (line = reader.readLine()) != null; ) {
          System.out.printf("[%-13s] %s%n", processName, line);
          if (consumer != null) {
            consumer.accept(line);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
