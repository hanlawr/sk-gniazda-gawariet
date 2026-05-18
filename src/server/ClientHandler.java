package server;

import com.google.gson.Gson;
import modele.ModelSesja;
import packet.Packet;
import packet.PacketEnum;

import java.io.*;
import java.net.Socket;
import java.util.List;

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
            case ADD_FRIEND:             handleAddFriend(packet);     break;
            case ACCEPT_FRIEND:          handleAcceptFriend(packet);  break;
            case REJECT_FRIEND:          handleRejectFriend(packet);  break;
            case FRIEND_LIST:            handleGetFriends();          break;
            case FRIEND_INVITE:          handleInvites();          break;
            default:
                send(error("nieznany typ pakietu " + packet.getType()));
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
        if (userManager.areFriends(loggedInUser, recipient)) {
            ModelSesja recipientSession = sessionManager.getSession(recipient);
            recipientSession.getWriter().println(
                    gson.toJson(new Packet(PacketEnum.RECEIVE_MESSAGE, loggedInUser, recipient, message))
            );

            send(success("Wiadomość dostarczono"));
        }else{
            send(error("użytkownik o loginie " + recipient + " i ty nie jesteście znajomymi"));
        }

    }



    private boolean requireLogin() {
        if (loggedInUser == null) {
            send(error("Musisz być zalogowany"));
            return false;
        }
        return true;
    }

    private Packet error(String msg) {
        return new Packet(PacketEnum.ERROR, "serwer", loggedInUser, msg);
    }

    private Packet success(String msg) {
        return new Packet(PacketEnum.SUCCESS, "server", loggedInUser, msg);
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
        try { socket.close();
        }
        catch (IOException ignored) {}
    }
    private void handleAddFriend(Packet packet) {
        if (!requireLogin()) return;

        String target = packet.getData();
        if (target == null || target.isBlank()) {
            send(error("podaj login użytkownika"));
            return;
        }
        if (target.equals(loggedInUser)) {
            send(error("nie możesz dodać siebie do znajomych"));
            return;
        }
        if (!userManager.userExists(target)) {
            send(error("użytkownik '" + target + "' nie istnieje"));
            return;
        }
        if (userManager.areFriends(loggedInUser, target)) {
            send(error("jesteście już znajomymi"));
            return;
        }
        if (userManager.hasPendingFriendRequest(loggedInUser, target)) {
            send(error("Użytkownik " + target + " już wysłał Ci zaproszenie. Zaakceptuj lub odrzuć je."));
            return;
        }
        if (userManager.sendFriendRequest(loggedInUser, target)) {
            send(success("zaproszenie wysłane do " + target));

            if (sessionManager.isOnline(target)) {
                sessionManager.getSession(target).getWriter().println(
                        gson.toJson(new Packet(PacketEnum.FRIEND_INVITE, "serwer", target, loggedInUser))
                );
            }
        } else {
            send(error("nie można wysłać zaproszenia do " + target));
        }
    }
    private void handleAcceptFriend(Packet packet) {
        if (!requireLogin()) return;

        String from = packet.getData();
        if (from == null) {
            send(error("brak danych"));
            return;
        }

        if (userManager.acceptFriendRequest(loggedInUser, from)) {
            send(success("zaakceptowano zaproszenie od " + from));

            if (sessionManager.isOnline(from)) {
                sessionManager.getSession(from).getWriter().println(
                        gson.toJson(new Packet(PacketEnum.NOTIFICATION, "server", from, loggedInUser + " zaakceptował Twoje zaproszenie!")));
            }
        } else {
            send(error("brak zaproszenia od " + from));
        }
    }

    private void handleRejectFriend(Packet packet) {
        if (!requireLogin()) return;

        String from = packet.getData();
        if (from == null) { send(error("Brak danych")); return; }

        if (userManager.rejectFriendRequest(loggedInUser, from)) {
            send(success("odrzucono zaproszenie od " + from));
        } else {
            send(error("brak zaproszenia od " + from));
        }
    }

    private void handleGetFriends() {
        if (!requireLogin()) return;

        List<String> friends = userManager.getFriends(loggedInUser);
        StringBuilder sb = new StringBuilder();
        if (friends.isEmpty()) {
            send(new Packet(PacketEnum.FRIEND_LIST, "SERVER", loggedInUser, ""));
            return;
        }
        for (String friend : friends) {
            String status = sessionManager.isOnline(friend) ? "online" : "offline";
            if (!sb.isEmpty()) sb.append(",");
            sb.append(friend).append(":").append(status);
        }
        send(new Packet(PacketEnum.FRIEND_LIST, "serwer", loggedInUser, sb.toString()));
    }
    private void handleInvites(){
        if (!requireLogin()) return;

        List<String> pendingInvites = userManager.getPendingFriends(loggedInUser);

        if (pendingInvites.isEmpty()) {
            send(new Packet(PacketEnum.FRIEND_INVITE, "Serwer", loggedInUser,  ""));
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String inviter : pendingInvites) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append(inviter);
        }
        send(new Packet(PacketEnum.FRIEND_INVITE, "serwer", loggedInUser, sb.toString()));
    }
}

