package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import modele.ModelUser;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserManage {
    private static final String DATA_FILE = "data/users.json"; //zapisywani użytkownicy w pliku .json

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create(); //obsługa plików .json przez bibliotekę GSON
    private Map<String, ModelUser> users = new ConcurrentHashMap<>(); //zbiór loginów i danych użytkowników
    //używane synchronized żeby odpowiednio czekać na wątki
    public UserManage() {
        loadUsers();
    }
    private synchronized void loadUsers() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            saveUsers();
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) { //utworzenie strumienu odczytu -> nie ma wycieków
            Type type = new TypeToken<Map<String, ModelUser>>(){}.getType(); //przypisanie żeby na pewno zapisywał w formacie mapy, klucz=login, wartość=model user
            Map<String, ModelUser> loaded = gson.fromJson(reader,type); //dopasowanie tego co jest w pliku do mapy
            if (loaded != null) {
                users = loaded; //pobranie z pliku
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private synchronized void saveUsers() {
        File file = new File(DATA_FILE);
        file.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    public synchronized boolean register(String login, String password) {
        if (users.containsKey(login)) return false;
        String hash = hashPassword(password);
        users.put(login, new ModelUser(login, hash));
        saveUsers();
        return true;
    }

    public synchronized boolean authenticate(String login, String password) {
        ModelUser user = users.get(login);
        if (user == null) return false;
        String loginHash=hashPassword(password);
        return loginHash.equals(user.getPasswordHash());
    }

    public synchronized boolean userExists(String login) {
        return users.containsKey(login);
    }


    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(password.getBytes(StandardCharsets.UTF_8))); //hashowanie haseł w pliku (dodatkowe bezpieczeństwo)
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("blad hashowania", e);
        }
    }
    public synchronized boolean areFriends(String login1, String login2) {
        ModelUser user = users.get(login1);
        if (user == null) return false;
        List<String> friends = user.getFriends();
        return friends != null && friends.contains(login2);
    }

    public synchronized boolean sendFriendRequest(String from, String to) {
        ModelUser target = users.get(to);
        if (target == null) return false;
        if (target.getFriends().contains(from)) return false;
        if (!target.getPendingFriends().contains(from)) {
            target.getPendingFriends().add(from);
            saveUsers();
        }
        return true;
    }

    public synchronized boolean acceptFriendRequest(String login, String from) {
        ModelUser user = users.get(login);
        ModelUser fromUser = users.get(from);
        if (user == null || fromUser == null) return false;
        if (!user.getPendingFriends().contains(from)) return false;

        user.getPendingFriends().remove(from);
        if (!user.getFriends().contains(from))       user.getFriends().add(from);
        if (!fromUser.getFriends().contains(login))  fromUser.getFriends().add(login);
        saveUsers();
        return true;
    }

    public synchronized boolean rejectFriendRequest(String login, String from) {
        ModelUser user = users.get(login);
        if (user == null) return false;
        boolean removed = user.getPendingFriends().remove(from);
        if (removed) saveUsers();
        return removed;
    }

    public synchronized List<String> getFriends(String login) {
        ModelUser user = users.get(login);
        if (user == null) return Collections.emptyList();
        List<String> friends = user.getFriends();
        return friends != null ? new ArrayList<>(friends) : Collections.emptyList();
    }
    public synchronized List<String> getPendingFriends(String login) {
        ModelUser user = users.get(login);
        if (user == null) return Collections.emptyList();
        List<String> pendingFriends = user.getPendingFriends();
        return pendingFriends != null ? new ArrayList<>(pendingFriends) : Collections.emptyList();
    }

    public synchronized boolean hasPendingFriendRequest(String login, String target) {
        ModelUser user = users.get(login);
        if (user == null) return false;
        return getPendingFriends(login).contains(target);
    }
}
