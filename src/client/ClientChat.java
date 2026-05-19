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


        //inicjowanie połączenia TCP z serwerem
        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            System.out.println("polaczono \n");


            //osobny watek na odbieranie wiadomosci
            ClientReciever receiver = new ClientReciever(reader);
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();

            printHelp();

            // petla do wysylania wiadomosci, w tym logowania, wysyłania packetu do serwera z prośbą o liste znajomych itd
            sendingLoop(receiver);

            //zamkniecie
            if (!socket.isClosed()) {
                socket.close();
            }
            System.out.println("rozlaczono.");
        }
        catch (Exception e) {
            System.err.println("blad : " + e.getMessage());
        }
    }


    // petla do wysylania packetów do servera
    private static void sendingLoop(ClientReciever receiver) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.print("> ");

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();

            //przywracanie > enterem
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
                    System.err.println("blad : " + e.getMessage());

                }
                receiver.stop();
                return;
            }

            //główna funkcja obsługująca co się dzieje
            processCommand(input);
        }
    }


    //przekształcanie danych (przez packet.toJson() ) w formę poprawną do przekształcenia do pliku .json przed wysłaniem do serwera
    private static void send(Packet packet) {
        if (writer != null) {
            writer.println(packet.toJson());
        }
    }

    private static boolean requireLogin() {
        if (currentUser == null) {
            System.out.println("musisz się zalogowac (login <login> <haslo>).");
            System.out.print("> ");
            return false;
        }
        return true;
    }

    //główna funkcja obsługująca co się dzieje, jaki packet wysyłany do servera
    private static void processCommand(String input) {
        String[] parts = input.split(" ", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "register":
                if (parts.length < 3) {
                    System.out.println("register <login> <haslo>");
                    return;
                }
                send(new Packet(PacketEnum.REGISTER, parts[1], "SERVER", parts[2]));
                break;
            case "login":
                if (parts.length < 3) {
                    System.out.println("login <login> <haslo>");
                    return; }
                currentUser = parts[1];
                send(new Packet(PacketEnum.LOGIN, parts[1], "SERVER", parts[2]));
                break;
            case "logout":
                if (currentUser == null) { System.out.println("nie jestes zalogowany.");
                    return;
                }
                send(new Packet(PacketEnum.LOGOUT, currentUser, "SERVER", null));
                currentUser = null;
                break;
            case "msg":
                if (parts.length < 3) {
                    System.out.println("msg <odbiorca> <tresc>");
                    return;
                }
                if (!requireLogin()) return;//tylko zalogowany może wysyłać
                send(new Packet(PacketEnum.SEND_MESSAGE, currentUser, parts[1], parts[2]));

                break;
            case "add":
                if (parts.length < 2) {
                    System.out.println("add <login>");

                }
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.ADD_FRIEND, currentUser, "SERVER", parts[1]));
                break;

            case "accept":
                if (parts.length < 2) {
                    System.out.println("accept <login>");
                }
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.ACCEPT_FRIEND, currentUser, "SERVER", parts[1]));

                break;

            case "reject":
                if (parts.length < 2) {
                    System.out.println("reject <login>");
                }
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.REJECT_FRIEND, currentUser, "SERVER", parts[1]));
                break;


            case "invites":
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.FRIEND_INVITE, currentUser, "SERVER", null));
                break;


            //wysyłanie prośby o  liste znajomych i ich aktualnego statusu do servera
            case "friends":
                if (!requireLogin()) return;
                send(new Packet(PacketEnum.FRIEND_LIST, currentUser, "SERVER", null));
                break;

            case "help":
                printHelp();
                break;
            default:
                System.out.println("nie ma takiej komendy. wpisz 'help' aby zobaczyc dostepne komendy.");
        }

    }


    //wyswietlanie pomocy
    private static void printHelp() {
        System.out.println(" komendy ");
        System.out.println(" register <login> <haslo> nowe konto ");
        System.out.println(" login <login> <haslo> logowanie ");
        System.out.println(" logout wylogowanie");
        System.out.println(" msg <odbiorca> <tresc> wyslij wiadomosc");
        System.out.println(" add <login>  zapros do znajomych");
        System.out.println(" accept <login>  zaakceptuj znajomego");
        System.out.println(" reject <login>  odrzuc znajomego");
        System.out.println(" invites wyswietl liste zaproszen do znajomych");
        System.out.println(" friends wyswietl liste swoich znajomych");
        System.out.println(" help pomoc ");
        System.out.println(" exit zakoncz ");

    }

   public static void main(String[] args) {
        //jesli nie zostaną podane argumenty przy uruchamianiu programu to ustawiane są defaultowe
        String host = args.length > 0 ? args[0] : "localhost"; //lub ip "10.81.66.9"
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 12347;
        new ClientChat(host, port).start();
    }



}