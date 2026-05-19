package modele;

import java.io.PrintWriter;
import java.net.Socket;

public class ModelSesja {
    private final String login;
    private final Socket socket;
    private final PrintWriter writer;

    public ModelSesja(String login, Socket socket, PrintWriter writer) {
        this.login = login;
        this.socket = socket;
        this.writer = writer;
    }

    /*public String getLogin()            { return login; }
    public Socket getSocket()           { return socket; }*/
    public PrintWriter getWriter()      { return writer; }
}
