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
            // jesli running=true i strumień nie jest zamknięty
            while (running && (line = reader.readLine()) != null) {
                System.out.println("\nSerwer: " + line);
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