import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.google.gson.Gson;

// pakiet do komunikacji między serwerem a klientem
public class Packet {
    private static final Gson gson = new Gson();
    private PacketEnum type; //określa typ pakietu
    private String sender;
    private String recipient;
    private String data;
    private String timestamp;

    public Packet() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    //ustawianie pól pakietu
    public Packet(PacketEnum type, String sender, String recipient, String data) {
        this();
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
    public String getTimestamp(){
        return timestamp;
    }
    public void setType(PacketEnum type){
        this.type = type;
    }
    public void setSender(String sender){
        this.sender = sender;
    }
    public void setRecipient(String recipient){
        this.recipient = recipient;
    }
    public void setData(String data){
        this.data = data;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static Packet fromJson(String json) {
        return gson.fromJson(json, Packet.class);
    }



}
