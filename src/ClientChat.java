import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientChat {
//NIE DZIAŁA DO KONCA BO SERWER JEST JEDNOWATKOWY NADAL
    public static void main(String[] args) throws Exception{
        Socket socket = new Socket("localhost", 12347);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));


        //osobny watek na odbieranie wiadomosci
        ClientReciever receiver = new ClientReciever(reader);
        Thread receiverThread = new Thread(receiver);
        receiverThread.start();

        Scanner sc = new Scanner(System.in);
        System.out.println("połączono \n");

        // petla do wysylania wiadomosci
        while (true) {
            if (sc.hasNextLine()) {
                String message = sc.nextLine();

                bufferedWriter.write(message);
                bufferedWriter.write("\n");
                bufferedWriter.flush();

                if (message.equalsIgnoreCase("END")) {
                    receiver.stop();
                    break;
                }
            }
        }

        socket.close();
        System.out.println("Rozłączono.");

    }


}