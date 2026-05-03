package client;

import packet.Packet;
import packet.PacketEnum;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;

public class ClientSender implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientSender.class.getName());

    private final PrintWriter writer;
    private final ClientReciever receiver;
    private volatile boolean running = true;
    private String currentUser = null;

    public ClientSender(PrintWriter writer, ClientReciever receiver) {
        this.writer = writer;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.print("> ");

        while (running && scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.print("> ");
                continue;
            }

            LOGGER.fine("komenda: " + input);

            if (input.equalsIgnoreCase("exit")) {
                if (currentUser != null) {
                    send(new Packet(PacketEnum.LOGOUT, currentUser, "SERVER", null));
                }
                // Zatrzymujemy oba wątki przy wyjściu
                stop();
                if (receiver != null) {
                    receiver.stop();
                }
                break;
            }

            processCommand(input);
            if (running) {
                System.out.print("> ");
            }
        }

    }

    private void send(Packet packet) {
        if (writer != null) {
            writer.println(packet.toJson());
        }
    }

    private boolean requireLogin() {
        if (currentUser == null) {
            System.out.println("musisz się zalogować (login <login> <hasło>).");
            return false;
        }
        return true;
    }

    private void processCommand(String input) {
        String[] parts = input.split(" ", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "register":
                if (parts.length < 3) {
                    System.out.println("register <login> <hasło>");
                    return;
                }
                send(new Packet(PacketEnum.REGISTER, parts[1], "SERVER", parts[2]));
                break;

            case "login":
                if (parts.length < 3) {
                    System.out.println("login <login> <hasło>");
                    return;
                }
                currentUser = parts[1];
                send(new Packet(PacketEnum.LOGIN, parts[1], "SERVER", parts[2]));
                break;

            case "logout":
                if (currentUser == null) {
                    System.out.println("nie jesteś zalogowany.");
                    return;
                }
                send(new Packet(PacketEnum.LOGOUT, currentUser, "SERVER", null));
                currentUser = null;
                break;

            case "msg":
                if (parts.length < 3) {
                    System.out.println("msg <odbiorca> <treść>");
                    return;
                }
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.SEND_MESSAGE, currentUser, parts[1], parts[2]));
                break;

            case "help":
                printHelp();
                break;

            default:
                System.out.println("nie ma takiej komendy. wpisz 'help' aby zobaczyć dostępne komendy.");
        }
    }

    private void printHelp() {
        System.out.println("  komendy                                        ");
        System.out.println("  register <login> <hasło>   nowe konto          ");
        System.out.println("  login    <login> <hasło>   logowanie           ");
        System.out.println("  logout                     wylogowanie         ");
        System.out.println("  msg <odbiorca> <treść>     wyślij wiadomość    ");
        System.out.println("  help                       pomoc               ");
        System.out.println("  exit                       zakończ             ");
    }

    public void stop() {
        running = false;
    }
}