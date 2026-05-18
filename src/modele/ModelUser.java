package modele;

import java.util.ArrayList;
import java.util.List;

public class ModelUser {
    private String login;
    private String passwordHash;
    private List<String> friends;
    private List<String> pendingFriends;
    public ModelUser(){
        this.friends=new ArrayList<>();
        this.pendingFriends= new ArrayList<>();
    }
    public ModelUser(String login, String passwordHash) {
        this();
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getLogin()                            { return login; }
    public String getPasswordHash()                     { return passwordHash; }
    public List<String> getFriends()                    {return friends;}
    public List<String> getPendingFriends()             {return pendingFriends; }
}
