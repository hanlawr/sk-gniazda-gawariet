package client;
import packet.Packet;
import java.io.BufferedReader;
import java.io.IOException;


// osobny wątek umożliwający odbiór wiadomości

public class ClientReciever implements Runnable {
    private final BufferedReader reader;
    private volatile boolean running = true; //czy ma działać, volatile żeby ne tworzyła się kopia, tylko było operowan ena tej wartości

    //konstruktor
    public ClientReciever(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public void run() {
        try {
            String line;
            Packet receivedPacket;
            //jesli running=true i strumień nie jest zamknięty
            while (running && (line = reader.readLine()) != null) {
                receivedPacket = Packet.fromJson(line);// recieved packet od serwera
                if (receivedPacket == null) continue;//pomijamy puste packety

                //rodzaj powiadomienia w zaleznosci od rodzaju packetu.
                switch (receivedPacket.getType()) {
                    case SUCCESS:
                        System.out.println("\rsukces: " + receivedPacket.getData());
                        System.out.print("> ");
                        break;

                    case ERROR:
                        System.err.println("\rerror: " + receivedPacket.getData());
                        System.out.print("> ");
                        break;

                    case NOTIFICATION:
                        System.err.println("\rpowiadomienie: " + receivedPacket.getData());
                        System.out.print("> ");
                        break;

                    case RECEIVE_MESSAGE:
                        System.out.println("\r" + receivedPacket.getSender() + ": " + receivedPacket.getData());
                        System.out.print("> ");
                        break;

                    case FRIEND_INVITE:
                        System.out.println("\r");
                        printInvites(receivedPacket.getData());
                        System.out.print("> ");
                        break;
                    case FRIEND_LIST:
                        System.out.println("\r");
                        printFriendList(receivedPacket.getData());
                        System.out.print("> ");
                        break;
                    default:
                        System.out.println("\nodebrano pakiet: " + receivedPacket.getType());
                        System.out.print("> ");
                        break;
                }
                System.out.flush();
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("\nutracono połączenie z serwerem.");
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (reader != null) reader.close();// zamknięcie strumienia
        } catch (IOException e) {
            System.err.println("błąd : " + e.getMessage());

        }
    }
    private void printFriendList(String data) {
        System.out.println("twoja lista znajomych:");
        if (data == null || data.isBlank()) {
            System.out.println("nie masz znajomych :c");
            return;
        }
        for (String entry : data.split(",")) //oddzielany znajomych przecinkami
        {
            String[] parts = entry.split(":");// status oddzielony dwukropkiem od loginu
            if (parts.length == 2) {
                String status = "online".equals(parts[1]) ? "online" : "offline";
                System.out.println("  " + status + " " + parts[0]);
            }
        }

    }

    private void printInvites(String data) {//analogicznie do printfriends
        System.out.println("twoje zaproszenia:");
        if (data == null || data.isBlank()) {
            System.out.println("nie masz zaproszen :c");
            return;
        }
        for (String inviter : data.split(",")) //oddzielany znajomych przecinkami
        {
                System.out.println(inviter +" chce cie dodac");
            }
        System.out.println("accept <login> żeby zaakceptować");
        System.out.println("reject <login>  żeby odrzucić ");

    }
}

