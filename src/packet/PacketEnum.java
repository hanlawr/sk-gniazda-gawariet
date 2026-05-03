package packet;

public enum PacketEnum {
    // stan
    LOGIN,
    LOGOUT,
    REGISTER,

    // opcje wiadomosci
    SEND_MESSAGE,
    RECEIVE_MESSAGE,

    // znajomi, to będzie potrzebne później
    //ADD_FRIEND,
    //ACCEPT_FRIEND,
    //REJECT_FRIEND,
    //FRIEND_LIST,
    //FRIEND_REQUEST_NOTIFICATION,//?

    // od systemu
    ERROR,
    SUCCESS,
    NOTIFICATION
}
