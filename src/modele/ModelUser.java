package modele;

public class ModelUser {
    private String login;
    private String passwordHash;
    public ModelUser(String login, String passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
    }
    public String getLogin()                            { return login; }
    public String getPasswordHash()                     { return passwordHash; }
}
