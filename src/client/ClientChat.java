package client;
import packet.Packet;
import packet.PacketEnum;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClientChat {
    private static String currentUser = null;
    private static String host;
    private static int port;
    private static PrintWriter writer;
    private static Socket socket;

    //konstruktor
    public ClientChat(String host, int port) {
        ClientChat.host = host;
        ClientChat.port = port;
    }

    public void start() {


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

            // petla do wysylania wiadomosci, w tym logowania, rejestrowania
            sendingLoop(receiver);

            //zamkniecie
            if (!socket.isClosed()) {
                socket.close();
            }
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


            //exit poza process command, bo gdyby tam to przy return wychodził by tylko z tej funkcji a nie z całego while
            if (input.equalsIgnoreCase("exit")) {
                if (currentUser != null) {
                    send(new Packet(PacketEnum.LOGOUT, currentUser, "SERVER", null));
                }
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("błąd : " + e.getMessage());

                }

                receiver.stop();
                return;
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
                    System.out.print("> ");
                    return;
                }
                send(new Packet(PacketEnum.REGISTER, parts[1], "SERVER", parts[2]));
                break;
            case "login":
                if (parts.length < 3) {
                    System.out.println("login <login> <hasło>");
                    System.out.print("> ");
                    return; }
                currentUser = parts[1];
                send(new Packet(PacketEnum.LOGIN, parts[1], "SERVER", parts[2]));
                break;
            case "logout":
                if (currentUser == null) { System.out.println("nie jesteś zalogowany.");
                    System.out.print("> ");
                    return;
                }
                send(new Packet(PacketEnum.LOGOUT, currentUser, "SERVER", null));
                currentUser = null;
                break;
            case "msg":
                if (parts.length < 3) {
                    System.out.println("msg <odbiorca> <treść>");
                    System.out.print("> ");
                    return;
                }
                if (!requireLogin()) return;//tylko zalogowany może wysyłać
                send(new Packet(PacketEnum.SEND_MESSAGE, currentUser, parts[1], parts[2]));
                System.out.print("> ");
                break;
            case "add":
                if (parts.length < 2) {
                    System.out.println("add <login>");

                }
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.ADD_FRIEND, currentUser, "SERVER", parts[1]));
                System.out.print("> ");
                break;

            case "accept":
                if (parts.length < 2) {
                    System.out.println("accept <login>"); }
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.ACCEPT_FRIEND, currentUser, "SERVER", parts[1]));
                System.out.print("> ");
                break;

            case "reject":
                if (parts.length < 2) {
                    System.out.println("reject <login>"); }
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.REJECT_FRIEND, currentUser, "SERVER", parts[1]));
                System.out.print("> ");
                break;


            case "invites":
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.FRIEND_INVITE, currentUser, "SERVER", null));
                System.out.print("> ");
                break;

            case "friends":
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.FRIEND_LIST, currentUser, "SERVER", null));
                System.out.print("> ");
                break;

            case "help":
                printHelp();
                System.out.print("> ");
                break;
            default:
                System.out.println("nie ma takiej komendy. wpisz 'help' aby zobaczyć dostępne komendy.");
                System.out.print("> ");
        }

    }


    //wyswietlanie pomocy
    private static void printHelp() {
        System.out.println(" komendy ");
        System.out.println(" register <login> <hasło> nowe konto ");
        System.out.println(" login <login> <hasło> logowanie ");
        System.out.println(" logout wylogowanie");
        System.out.println(" msg <odbiorca> <treść> wyślij wiadomość");
        System.out.println(" add <login>  zaproś do znajomych");
        System.out.println(" accept <login>  zaakceptuj znajomego");
        System.out.println(" reject <login>  odrzuć znajomego");
        System.out.println(" invites wyświetl listę zaproszen do znajomych");
        System.out.println(" friends wyświetl listę swoich znajomych");
        System.out.println(" help pomoc ");
        System.out.println(" exit zakończ ");

    }

   public static void main(String[] args) {
        //jesli nie zostaną podane argumenty przy uruchamianiu programu to ustawiane są defaultowe
        String host = args.length > 0 ? args[0] : "localhost"; //lub ip "10.81.66.9"
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 12347;
        new ClientChat(host, port).start();
    }



}