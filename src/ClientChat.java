import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientChat {
//NIE DZIAŁA DO KONCA BO SERWER JEST JEDNOWATKOWY NADAL
    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);
        String username;

        //logowanie i nawiazywanie polaczenia
        System.out.print("Podaj login: ");
        username = sc.nextLine();
        System.out.print("Podaj hasło: ");
        String password = sc.nextLine();


        try {
            Socket socket = new Socket("localhost", 12347);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            //osobny watek na odbieranie wiadomosci
            ClientReciever receiver = new ClientReciever(reader);
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();

            Packet loginPacket = new Packet(PacketEnum.LOGIN, username, "Serwer", "");
            bufferedWriter.write(loginPacket.toJson());
            bufferedWriter.newLine();
            bufferedWriter.flush();
            System.out.println("połączono \n");
            System.out.println("wiadomości wpisuj w formacie \n odbiorca:treść \n lub wpisz 'END' by wyjść");

            // petla do wysylania wiadomosci
            while (true) {
                if (sc.hasNextLine()) {
                    String message = sc.nextLine();

                    if (message.equalsIgnoreCase("END")) {
                        Packet logoutPacket = new Packet(PacketEnum.LOGOUT, username, "Serwer", "");
                        bufferedWriter.write(message);
                        bufferedWriter.write("\n");
                        bufferedWriter.flush();
                        receiver.stop();
                        break;
                    }
                    //formatowanie wiadomości
                    if (message.contains(":")) {
                        String[] parts = message.split(":", 2);
                        String recipient = parts[0].trim();
                        String messageText = parts[1].trim();

                        Packet msgPacket = new Packet(PacketEnum.SEND_MESSAGE, username, recipient, messageText);//packet z wysyłaną wiadomością
                        bufferedWriter.write(msgPacket.toJson());
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    }
                    else {
                        System.out.println("zły format wiadomości! Użyj formatu - odbiorca:treść");
                    }

                }
            }

            socket.close();
            System.out.println("rozłączono.");

        }
        catch (Exception e) {
            System.err.println("błąd : " + e.getMessage());
        }


    }

}