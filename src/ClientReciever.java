import java.io.BufferedReader;
import java.io.IOException;


/* osobny wątek umożliwający odbiór wiadomości bez blokowania pisania */

public class ClientReciever implements Runnable {
    private BufferedReader reader;
    private volatile boolean running = true; //czy ma działać, volatile żeby ne tworzyła się kopia, tylko było operowan ena tej wartości


    public ClientReciever(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public void run() {
        try {
            String line;
            Packet receivedPacket = null;
            // jesli running=true i strumień nie jest zamknięty
            while (running && (line = reader.readLine()) != null) {
                receivedPacket = Packet.fromJson(line);
                if (receivedPacket == null) continue;

                //rodzaj powiadomienia w zaleznosci od rodzaju packetu
                switch (receivedPacket.getType()) {
                    case SUCCESS:
                        System.out.println("\nsuccess: " + receivedPacket.getData());
                        break;

                    case ERROR:
                        System.err.println("\nerror: " + receivedPacket.getData());
                        break;

                    case RECEIVE_MESSAGE:
                        System.out.println("\n[" + receivedPacket.getSender() + "]: " + receivedPacket.getData());
                        break;

                    /*case FRIEND_LIST:
                        System.out.println("\nlista znajomych:");
                        System.out.println(receivedPacket.getData());
                        System.out.println("\n");
                        break;*/

                    case NOTIFICATION:
                        System.out.println("\nnontification: " + receivedPacket.getData());
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
}