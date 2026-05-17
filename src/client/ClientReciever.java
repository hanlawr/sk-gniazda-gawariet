package client;
import packet.Packet;

import java.io.BufferedReader;
import java.io.IOException;


// osobny wątek umożliwający odbiór wiadomości

public class ClientReciever implements Runnable {
    private final BufferedReader reader;
    private volatile boolean running = true; //czy ma działać, volatile żeby ne tworzyła się kopia, tylko było operowan ena tej wartości


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

                //rodzaj powiadomienia w zaleznosci od rodzaju packetu
                switch (receivedPacket.getType()) {
                    case SUCCESS:
                        System.out.println("\nsukces: " + receivedPacket.getData());
                        break;

                    case ERROR:
                        System.err.println("\nerror: " + receivedPacket.getData());
                        break;

                    case NOTIFICATION:
                        System.err.println("\npowiadomienie: " + receivedPacket.getData());
                        break;

                    case RECEIVE_MESSAGE:
                        System.out.println("\n" + receivedPacket.getSender() + ": " + receivedPacket.getData());
                        break;


                    case FRIEND_INVITE:
                            printInvites(receivedPacket.getData());
                        break;
                    case FRIEND_LIST:
                        printFriendList(receivedPacket.getData());
                        break;
                    default:
                        System.out.println("\nodebrano pakiet: " + receivedPacket.getType());
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
                String icon = "online".equals(parts[1]) ? "online" : "offline";
                System.out.println("  " + icon + " " + parts[0]);
            }
        }
        System.out.print("> ");
    }

    private void printInvites(String data) {//analogicznie do printfriends
        System.out.println("twoje zaproszenia:");
        if (data == null || data.isBlank()) {
            System.out.println("nie masz zaproszen :c");
            return;
        }
        for (String inviter : data.split(",")) //oddzielany znajomych przecinkami
        {
                System.out.println("\n"+ inviter +" chce cie dodac");
            }
        System.out.println("accept <login> żeby zaakceptować");
        System.out.println("reject <login>  żeby odrzucić ");
        System.out.print("> ");
    }
}

