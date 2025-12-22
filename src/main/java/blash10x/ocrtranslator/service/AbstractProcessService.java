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
      System.out.printf("%s has started: %s", processName, command);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (!process.isAlive()) {
          System.out.printf("%s has already been terminated.\n", processName);
          return;
        }
        close();
      }));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void close() {
    OutputStream os = process.getOutputStream(); // 프로세스의 입력 스트림 가져오기
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
      writer.write("q\n");
      writer.flush(); // 버퍼 비우기 (중요: 데드락 발생 가능성)

      int exitCode = process.waitFor();
      System.out.printf("%s has closed: exitCode=%d\n", processName, exitCode);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      process.children().forEach(ProcessHandle::destroy);
      process.destroy();
    }
  }
}
