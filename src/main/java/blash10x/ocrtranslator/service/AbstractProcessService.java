package blash10x.ocrtranslator.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;

/**
 * Author: myungsik.sung@gmail.com
 */
public abstract class AbstractProcessService {
  protected String processName;
  protected Process process;

  protected AbstractProcessService(String processName) {
    this.processName = processName;
  }

  protected void start(String command) {
    start(command, null);
  }

  protected void start(String command, Redirect outputRedirect) {
    try {
      ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);

      //builder.redirectInput(ProcessBuilder.Redirect.PIPE); // Default: Redirect.PIPE
      if (outputRedirect != null) {
        builder.redirectOutput(outputRedirect); // Default: Redirect.PIPE
      }
      builder.redirectErrorStream(true);

      process = builder.start();
      System.out.printf("%s has started: %s%n", processName, command);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
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
    OutputStream pos = process.getOutputStream(); // 프로세스의 입력 스트림 가져오기
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(pos));
    try {
      writer.write(str);
      writer.flush(); // 버퍼 비우기 (중요: 데드락 발생 가능성)
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void close() {
    writeToProcess("q\n");
    try {
      int exitCode = process.waitFor();
      System.out.printf("%s has closed: exitCode=%d%n", processName, exitCode);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      process.children().forEach(ProcessHandle::destroy);
      process.destroy();
    }
  }
/*
  private class OutputHandler implements Runnable {
    private final Consumer<String> consumer;

    public OutputHandler(Consumer<String> consumer) {
      this.consumer = consumer;
    }

    public void run() {
      InputStream pis = process.getInputStream();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(pis, StandardCharsets.UTF_8))) {
        for (String line; (line = reader.readLine()) != null; ) {
          System.out.printf("[%-11s] %s%n", processName, line);
          if (consumer != null) {
            consumer.accept(line);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }*/
}
