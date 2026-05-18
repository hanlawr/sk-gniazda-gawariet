package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ServerChat {
    private int PORT;
    private static final int MAX_CLIENTS = 10;


    private final UserManage  userManager;
    private final SessionManage sessionManager;
    private final ExecutorService threadPool;
    private final Semaphore connectionSlots;

    public ServerChat(int port) {
        this.userManager     = new UserManage();
        this.sessionManager  = new SessionManage();
        this.threadPool      = Executors.newFixedThreadPool(MAX_CLIENTS);
        this.connectionSlots = new Semaphore(MAX_CLIENTS);
        this.PORT=port;
    }

    public void start() {
        System.out.println("Uruchomiono serwer");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                if (connectionSlots.tryAcquire()) {
                    threadPool.execute(() -> {
                        try {
                            new ClientHandler(clientSocket, userManager, sessionManager).run();

                        } finally {
                            connectionSlots.release();
                        }
                    });
                } else {
                    System.out.println("Odrzucono połączenie " + clientSocket.getInetAddress());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        threadPool.shutdown();
    }

    public static void main(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[0]) : 12347;
        new ServerChat(port).start();
    }
}
