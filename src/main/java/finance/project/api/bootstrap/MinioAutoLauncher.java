package finance.project.api.bootstrap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

@Component
public class MinioAutoLauncher {

    private static final String MINIO_PATH = "C:\\minio\\minio.exe";
    private static final String DATA_PATH = "C:\\minio\\data";
    private static final int MINIO_PORT = 9000;

    private Process minioProcess;

    @PostConstruct
    public void startMinioIfNotRunning() {
        if (!new File(MINIO_PATH).exists()) {
            System.out.println("[MinIO] Skipping auto-start: " + MINIO_PATH + " not found");
            return;
        }

        if (isPortOpen("localhost", MINIO_PORT)) {
            System.out.println("[MinIO] Already running on port " + MINIO_PORT);
            return;
        }

        System.out.println("[MinIO] Starting MinIO server...");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    MINIO_PATH,
                    "server", DATA_PATH,
                    "--console-address", ":9090"
            );
            pb.directory(new File("C:\\minio"));
            pb.inheritIO(); // affiche la sortie dans la console Spring Boot
            minioProcess = pb.start();
            Thread.sleep(4000); // petit délai pour qu'il démarre avant l'import
            System.out.println("[MinIO] Launched successfully.");
        } catch (IOException | InterruptedException e) {
            System.err.println("[MinIO] Failed to start: " + e.getMessage());
        }
    }
    /*
    @PreDestroy
    public void stopMinioOnExit() {
        if (minioProcess != null && minioProcess.isAlive()) {
            System.out.println("[MinIO] Stopping MinIO...");
            minioProcess.destroy();
            try {
                if (!minioProcess.waitFor(3, TimeUnit.SECONDS)) {
                    minioProcess.destroyForcibly();
                }
                System.out.println("[MinIO] Stopped.");
            } catch (InterruptedException ignored) {}
        }
    }*/

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
