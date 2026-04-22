import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerChat {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12347);
        while (true) {
            Socket socket = serverSocket.accept();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedWriter.write("Napisz end : by zakończyć połączenie.\n");
            bufferedWriter.flush();
            String line = " ";
            Scanner sc = new Scanner(System.in);


            while (!line.contains("END")) {

                String line2 = sc.nextLine();
                bufferedWriter.write(line2);
                bufferedWriter.write("\n");
                bufferedWriter.flush();
                line = bufferedReader.readLine();
                System.out.println(line);
                System.out.flush();


            }


            socket.close();
        }


    }
}
