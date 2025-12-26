package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.App;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import lombok.Getter;

/**
 * Author: myungsik.sung@gmail.com
 */
public abstract class AbstractProcessService {
  private static final ExecutorService executorService = App.EXECUTOR_SERVICE;
  @Getter
  private final String processName;
  private Process process;
  private BufferedWriter writer;

  protected AbstractProcessService(String processName) {
    this.processName = processName;
  }

  protected void start(String command, Consumer<String> outputConsumer) {
    try {
      ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);

      //builder.redirectInput(ProcessBuilder.Redirect.PIPE); // Default: Redirect.PIPE
      //builder.redirectOutput(ProcessBuilder.Redirect.PIPE); // Default: Redirect.PIPE
      builder.redirectErrorStream(true);

      process = builder.start();
      executorService.execute(new ProcessOutputHandler(outputConsumer));
      writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream())); // get stream for input to the process
      System.out.printf("%s has started: %s%n", processName, command);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        executorService.shutdown();

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
    writeToProcess("q\n"); // termination command: 'q'
    try {
      writer.close();
      int exitCode = process.waitFor();
      System.out.printf("%s has closed: exitCode=%d%n", processName, exitCode);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private class ProcessOutputHandler implements Runnable {
    private final Consumer<String> consumer;

    public ProcessOutputHandler(Consumer<String> consumer) {
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
        System.out.printf("%s%n", e.getMessage());
        throw new RuntimeException(e);
      } finally {
        System.out.printf("Exit OutputHandler: %s%n", processName);
      }
    }
  }
}
