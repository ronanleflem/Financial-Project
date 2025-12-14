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

    private static final int MINIO_PORT = 9000;
    private static final int CONSOLE_PORT = 9090;

    // Windows
    private static final String MINIO_PATH_WIN = "C:\\minio\\minio.exe";
    private static final String DATA_PATH_WIN  = "C:\\minio\\data";
    private static final String WORKDIR_WIN    = "C:\\minio";

    // Linux
    private static final String MINIO_PATH_LINUX = "/usr/local/bin/minio";
    private static final String DATA_PATH_LINUX  = "/home/ronan/minio/data"; // ajuste si tu veux ailleurs
    private static final String WORKDIR_LINUX    = "/usr/local/minio";

    private Process minioProcess;

    @PostConstruct
    public void startMinioIfNotRunning() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        String minioPath = isWindows ? MINIO_PATH_WIN : MINIO_PATH_LINUX;
        String dataPath  = isWindows ? DATA_PATH_WIN  : DATA_PATH_LINUX;
        String workDir   = isWindows ? WORKDIR_WIN    : WORKDIR_LINUX;

        if (!new File(minioPath).exists()) {
            System.out.println("[MinIO] Skipping auto-start: " + minioPath + " not found");
            return;
        }

        if (isPortOpen("localhost", MINIO_PORT)) {
            System.out.println("[MinIO] Already running on port " + MINIO_PORT);
            return;
        }

        // crée le dossier data si besoin
        File dataDir = new File(dataPath);
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            System.out.println("[MinIO] Skipping auto-start: cannot create data dir: " + dataDir.getAbsolutePath());
            return;
        }

        System.out.println("[MinIO] Starting MinIO server...");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    minioPath,
                    "server", dataPath,
                    "--address", ":" + MINIO_PORT,
                    "--console-address", ":" + CONSOLE_PORT
            );

            File wd = new File(workDir);
            if (wd.exists()) pb.directory(wd);

            pb.inheritIO();
            minioProcess = pb.start();

            Thread.sleep(3000);
            System.out.println("[MinIO] Launched successfully.");
        } catch (IOException | InterruptedException e) {
            System.err.println("[MinIO] Failed to start: " + e.getMessage());
        }
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
