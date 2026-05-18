package packet;
import com.google.gson.Gson;

// pakiet do komunikacji między serwerem a klientem
public class Packet {
    static final Gson gson = new Gson();
    private PacketEnum type; //określa typ pakietu
    private String sender;
    private String recipient;
    private String data;

    //konstruktor z ustawianiem pól pakietu
    public Packet(PacketEnum type, String sender, String recipient, String data) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.data = data;
    }

    //funkcje do zwracania odpowiednik pól pakietu
    public PacketEnum getType(){
        return type;
    }
    public String getSender(){
        return sender;
    }
    public String getRecipient(){
        return recipient;
    }
    public String getData(){
        return data;
    }
    public String toJson() {
        return gson.toJson(this);
    }

    public static Packet fromJson(String json) {
        return gson.fromJson(json, Packet.class);
    }



}
