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
    ADD_FRIEND,
    ACCEPT_FRIEND,
    REJECT_FRIEND,
    FRIEND_LIST,
    FRIEND_INVITE, //zaproszenie, zmieniłam z notification na invite zeby bylo bardzej intuicyjne


    // od systemu
    ERROR,
    SUCCESS,
    NOTIFICATION
}
