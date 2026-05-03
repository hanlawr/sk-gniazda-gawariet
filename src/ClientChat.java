import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.*;


public class ClientChat {
    static Logger LOGGER = Logger.getLogger(ClientChat.class.getName());
    private static String currentUser = null;

    private static String host;
    private static int port;
    private final Gson gson = new Gson();
    private static Socket       socket;
    private static PrintWriter  writer;

    //konstruktor
    public ClientChat(String host, int port) {
        this.host = host;
        this.port = port;
    }


    public void start() {
        Scanner sc = new Scanner(System.in);
        String username;

        //najpierw łączę z serwerem
        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            System.out.println("połączono \n");


            //osobny watek na odbieranie wiadomosci
            ClientReciever receiver = new ClientReciever(reader);
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();

            printHelp();
            // petla do wysylania wiadomosci, w tym logowania
            sendingLoop(receiver);

            //zamkniecie
            socket.close();
            System.out.println("rozłączono.");

        }
        catch (Exception e) {
            System.err.println("błąd : " + e.getMessage());
        }

    }


    private static void sendingLoop(ClientReciever receiver) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.print("> ");

        while (scanner.hasNextLine()) {
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
                receiver.stop();
                break;
            }

            //główna funkcja obsługująca co się dzieje
            processCommand(input);
            System.out.print("> ");
        }
    }

    private static void send(Packet packet) {
        if (writer != null) {
            writer.println(packet.toJson());
        }
    }


    private static boolean requireLogin() {
        if (currentUser == null) {
            System.out.println("musisz się zalogować (login <login> <hasło>).");
            return false;
        }
        return true;
    }


//główna funkcja obsługująca co się dzieje
    private static void processCommand(String input) {
        String[] parts = input.split(" ", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {

            case "register":
                if (parts.length < 3) {
                    System.out.println("register <login> <hasło>");
                    return; }
                send(new Packet(PacketEnum.REGISTER, parts[1], "SERVER", parts[2]));
                break;

            case "login":
                if (parts.length < 3) {
                    System.out.println("register <login> <hasło>");
                    return; }
                currentUser = parts[1];
                send(new Packet(PacketEnum.LOGIN, parts[1], "SERVER", parts[2]));
                break;

            case "logout":
                if (currentUser == null) { System.out.println("nie jesteś zalogowany.");
                    return; }
                send(new Packet(PacketEnum.LOGOUT, currentUser, "SERVER", null));
                currentUser = null;
                break;

            case "msg":
                if (parts.length < 3) {
                    System.out.println("msg <odbiorca> <treść>");
                    return; }
                if (!requireLogin()) return;//tylko zalogowany może wysyłać
                send(new Packet(PacketEnum.SEND_MESSAGE, currentUser, parts[1], parts[2]));
                break;

            case "help":
                printHelp();
                break;

            default:
                System.out.println("nie ma takiej komendy. wpisz 'help' aby zobaczyć dostępne komendy.");
        }
    }


    //wyswietlanie pomocy
    private static void printHelp() {
        System.out.println("  komendy                                        ");
        System.out.println("  register <login> <hasło>   nowe konto          ");
        System.out.println("  login    <login> <hasło>   logowanie           ");
        System.out.println("  logout                     wylogowanie         ");
        System.out.println("  msg <odbiorca> <treść>     wyślij wiadomość    ");
        //System.out.println("  addfriend <login>          zaproś do znajomych ");
        //System.out.println("  accept <login>             akceptuj zaproszenie");
        //System.out.println("  reject <login>             odrzuć zaproszenie  ");
        //System.out.println("  friends                    lista znajomych     ");
        System.out.println("  help                       pomoc               ");
        System.out.println("  exit                       zakończ             ");


    }

    private static void configureLogging() {
        LogManager.getLogManager().reset();
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        try {
            new java.io.File("logs").mkdirs();
            FileHandler fh = new FileHandler("logs/client.log", true);
            fh.setLevel(Level.ALL);
            fh.setFormatter(new SimpleFormatter());
            root.addHandler(fh);
        } catch (IOException e) {
            System.err.println("nie można otworzyć pliku logów klienta: " + e.getMessage());
        }
    }





    public static void main(String[] args) {
        configureLogging();
        String host = args.length > 0 ? args[0] : "localhost";
        int    port = args.length > 1 ? Integer.parseInt(args[1]) : 12347;
        new ClientChat(host, port).start();
    }

}