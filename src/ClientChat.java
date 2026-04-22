import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientChat {
    public static void main(String[] args) throws Exception{
        Socket socket = new Socket("localhost", 12347);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Scanner sc = new Scanner(System.in);
        String line=" ";
        line = reader.readLine();
        System.out.println(line);
        System.out.flush();

        while (!line.contains("END")){
            line = reader.readLine();
            System.out.println(line);
            System.out.flush();
            String line2=sc.nextLine();
            bufferedWriter.write(line2);
            bufferedWriter.write("\n");
            bufferedWriter.flush();



        }


    }
}
