package day18;



import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

// 초대하기 기능 추가예정
public class ChatServer {
    public static Set<String> nicknameMemory = new HashSet<>();
    public static Map<Integer, Map<String, PrintWriter>> chatRooms = new HashMap<>();
    public static Map<Integer, List<String>> roomClients = new HashMap<>();
    public static Map<Integer, String> roomNames = new HashMap<>();


    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ServerThread(socket).start();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}




