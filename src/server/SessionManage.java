package server;

import modele.ModelSesja;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManage {

        private final Map<String, ModelSesja> sessions = new ConcurrentHashMap<>(); //zbiera i przechowuje loginy i sesje

        public void addSession(String login, Socket socket, PrintWriter writer) {
            sessions.put(login, new ModelSesja(login, socket, writer)); //łączy login z socketem i writerem
        }

        public void removeSession(String login) {
            sessions.remove(login);
        }

        public boolean isOnline(String login) {
            ModelSesja s = sessions.get(login);
            //sprawdza w mapie czy taki login jest teraz zarejestrowany jako (login, sesja)
            return s != null;
        }

        public ModelSesja getSession(String login) {
            return sessions.get(login); //dla tej sesji -> ten login
        }
    }

