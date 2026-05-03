package server;

import com.google.gson.Gson;
import modele.ModelSesja;
import packet.Packet;
import packet.PacketEnum;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable{

    private final Socket socket;
    private final UserManage userManager;
    private final SessionManage sessionManager;
    private final Gson gson = new Gson();

    private PrintWriter writer;
    private String loggedInUser = null;

    public ClientHandler(Socket socket, UserManage userManager, SessionManage sessionManager) {
        this.socket = socket;
        this.userManager = userManager;
        this.sessionManager = sessionManager;
    }
    @Override
    public void run() {
        try {
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                Packet packet = gson.fromJson(line, Packet.class);
                handlePacket(packet);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            cleanup();
        }
    }
    private void handlePacket(Packet packet) {
        if (packet == null || packet.getType() == null) return;

        switch (packet.getType()) {
            case LOGIN:                  handleLogin(packet);         break;
            case REGISTER:               handleRegister(packet);      break;
            case LOGOUT:                 handleLogout();              break;
            case SEND_MESSAGE:           handleSendMessage(packet);   break;
            default:
                send(error("Nieznany typ pakietu: " + packet.getType()));
        }
    }

    private void handleLogin(Packet packet) {
        String login    = packet.getSender();
        String password = packet.getData();

        if (login == null || password == null) {
            send(error("Brak danych logowania")); return;
        }
        if (!userManager.userExists(login)) {
            send(error("Użytkownik nie istnieje")); return;
        }
        if (!userManager.authenticate(login, password)) {
            send(error("Nieprawidłowe hasło")); return;
        }
        if (sessionManager.isOnline(login)) {
            send(error("Użytkownik już zalogowany")); return;
        }

        loggedInUser = login;
        sessionManager.addSession(login, socket, writer);
        send(success("Zalogowano jako " + login));
    }

    private void handleRegister(Packet packet) {
        String login    = packet.getSender();
        String password = packet.getData();

        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            send(error("login i hasło są puste.")); return;
        }
        if (userManager.register(login, password)) {
            send(success("jesteś zarejestrowany. teraz zaloguj się login <login> <hasło>"));
        } else {
            send(error("login '" + login + "' jest już w użyciu"));
        }
    }

    private void handleLogout() {
        if (loggedInUser != null) {
            sessionManager.removeSession(loggedInUser);
            loggedInUser = null;
        }
        send(success("Wylogowano"));
    }

    private void handleSendMessage(Packet packet) {
        if (!requireLogin()) return;

        String recipient = packet.getRecipient();
        String message   = packet.getData();

        if (recipient == null || message == null || message.isBlank()) {
            send(error("nieprawidłowe dane wiadomości.")); return;
        }
        if (!userManager.userExists(recipient)) {
            send(error("użytkownik o loginie'" + recipient + "' nie istnieje")); return;
        }
        if (!sessionManager.isOnline(recipient)) {
            send(error("użytkownik o loginie '" + recipient + "' jest teraz offline")); return;
        }

        ModelSesja recipientSession = sessionManager.getSession(recipient);
        recipientSession.getWriter().println(
                gson.toJson(new Packet(PacketEnum.RECEIVE_MESSAGE, loggedInUser, recipient, message))
        );

        send(success("Wiadomość dostarczono"));
    }



    private boolean requireLogin() {
        if (loggedInUser == null) {
            send(error("Musisz być zalogowany"));
            return false;
        }
        return true;
    }

    private Packet error(String msg) {
        return new Packet(PacketEnum.ERROR, "SERVER", loggedInUser, msg);
    }

    private Packet success(String msg) {
        return new Packet(PacketEnum.SUCCESS, "SERVER", loggedInUser, msg);
    }

    private void send(Packet packet) {
        if (writer != null) {
            writer.println(gson.toJson(packet));
            writer.flush();
        }

    }

    private void cleanup() {
        if (loggedInUser != null) {
            sessionManager.removeSession(loggedInUser);
        }
        try { socket.close(); } catch (IOException ignored) {}
    }
}

