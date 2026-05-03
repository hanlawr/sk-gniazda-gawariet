package client;
//import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.*;


public class ClientChat {
    private static String host;
    private static int port;

    //konstruktor
    public ClientChat(String host, int port) {
        ClientChat.host = host;
        ClientChat.port = port;
    }


    public void start() {

        //najpierw łączę z serwerem
        try {
            Socket socket = new Socket(host, port);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            System.out.println("połączono \n");

            //osobny watek na odbieranie wiadomosci
            ClientReciever receiver = new ClientReciever(reader);
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();

            printHelp();
            // osobny wątek na wysyłanie wiadomości
            ClientSender sender = new ClientSender(writer, receiver);
            Thread senderThread = new Thread(sender);
            senderThread.start();
            //zamkniecie dopiero jako w wysylajacym pojawi sie exit
            senderThread.join();

            //zamkniecie
            socket.close();
            System.out.println("rozłączono.");

        }
        catch (Exception e) {
            System.err.println("błąd : " + e.getMessage());
        }


    }



    //wyswietlanie pomocy
    private static void printHelp() {
        System.out.println("  komendy                                        ");
        System.out.println("  register <login> <hasło>   nowe konto          ");
        System.out.println("  login    <login> <hasło>   logowanie           ");
        System.out.println("  logout                     wylogowanie         ");
        System.out.println("  msg <odbiorca> <treść>     wyślij wiadomość    ");
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