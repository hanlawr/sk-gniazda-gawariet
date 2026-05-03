package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class ServerChat {
    private static final int PORT        = 12347;
    private static final int MAX_CLIENTS = 10;

    private static final Logger LOGGER = Logger.getLogger(ServerChat.class.getName());

    private final UserManage  userManager;
    private final SessionManage sessionManager;
    private final ExecutorService threadPool;
    private final Semaphore connectionSlots;

    public ServerChat() {
        this.userManager     = new UserManage();
        this.sessionManager  = new SessionManage();
        this.threadPool      = Executors.newFixedThreadPool(MAX_CLIENTS);
        this.connectionSlots = new Semaphore(MAX_CLIENTS);
    }

    public void start() {
        LOGGER.info("sewer uruchomiony na " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                if (connectionSlots.tryAcquire()) {
                    LOGGER.info("Nowe połączenie: " + clientSocket.getInetAddress());
                    threadPool.execute(() -> {
                        try {
                            new ClientHandler(clientSocket, userManager, sessionManager).run();
                        } finally {
                            connectionSlots.release();
                        }
                    });
                } else {
                    LOGGER.warning("Odrzucono połączenie " + clientSocket.getInetAddress());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Błąd serwera: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        threadPool.shutdown();
        LOGGER.info("Serwer zatrzymany.");
    }

    public static void main(String[] args) {
        new ServerChat().start();
    }
}
