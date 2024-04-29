package day18;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class ServerThread extends Thread {
    private Socket socket;
    private String nickname;
    private BufferedReader in;
    private PrintWriter out;
    private int currentRoom = -1;
    private static int roomCounter = 1;
    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            nickname = in.readLine();
            while (chackNickname(nickname)) {
                out.println(nickname + "은 이미 존재하는 닉네임 입니다. 다른 닉네임을 입력해주세요");
                nickname = in.readLine();
            }
            addNickname(nickname);
            out.println(nickname + "님이 서버에 접속했습니다.");
            System.out.println(nickname + "님이 서버에 접속했습니다. IP : " + socket.getInetAddress());


            //메뉴얼 보내기
            List<Manual> manualList = new ArrayList<>();
            manualList.add(new Manual("방 목록 보기", "/list"));
            manualList.add(new Manual("전체 접속자 목록 보기", "/users"));
            manualList.add(new Manual("현재 방 접속자 보기", "/roomusers"));
            manualList.add(new Manual("방 생성", "/create"));
            manualList.add(new Manual("방 입장", "/join [방번호]"));
            manualList.add(new Manual("방 이름 변경하기", "/renameroom [방이름]"));
            manualList.add(new Manual("방 나가기", "/exit"));
            manualList.add(new Manual("접속종료", "/bye"));
            manualList.add(new Manual("귓속말", "/whisper [닉네임] [메시지]"));
            String startMsg = "명령어를 보기 위해서는 '/help'를 입력해주세요";
            String wrongMsg = "잘못된 명령어를 입력하셨습니다.";
            out.println(startMsg);
            out.println(manualList);

            String line;
            while ((line = in.readLine()) != null) {
                if ("/list".equalsIgnoreCase(line.trim())) {
                    listRooms();
                } else if ("/users".equalsIgnoreCase(line.trim())){
                    Set<String> allusers = new HashSet<>(ChatServer.nicknameMemory);
                    out.println("현재 접속 중인 사용자");
                    Iterator<String> usersIter = allusers.iterator();
                    while (usersIter.hasNext()){
                        String user = usersIter.next();
                        System.out.println(user);
                    }
                } else if ("/roomusers".equalsIgnoreCase(line.trim())){
                    if (currentRoom != -1) {
                        List<String> roomUsers = ChatServer.roomClients.get(currentRoom);
                        out.println(currentRoom + " 번 방 사용자 목록:");
                        for (String user : roomUsers) {
                            out.println(user);
                        }
                    } else {
                        out.println("먼저 채팅방에 입장해주세요.");
                    }
                } else if ("/create".equalsIgnoreCase(line.trim())) {
                    if (currentRoom == -1) {
                        currentRoom = createRoom();
                        joinRoom(currentRoom, nickname, out);
                    } else {
                        out.println("현재 채팅방에서 나가셔야 새로운 방을 생성할 수 있습니다.");
                    }
                } else if (line.startsWith("/join")) {
                    String[] numbers = line.split(" ");
                    if (numbers.length == 2) {
                        int roomNumber = Integer.parseInt(numbers[1]);
                        if (currentRoom == -1) {
                            currentRoom = roomNumber;
                            joinRoom(currentRoom, nickname, out);
                        } else {
                            out.println("현재 채팅방에서 나가셔야 다른 방으로 이동할 수 있습니다.");
                        }
                    } else {
                        out.println("방 번호를 입력해주세요. 예) /join [방번호]");
                    }
                } else if (line.startsWith("/renameroom")) {
                    String[] parts = line.split(" ", 2);
                    if (parts.length == 2) {
                        String newRoomName = parts[1];
                        if (currentRoom != -1) {
                            ChatServer.roomNames.put(currentRoom, newRoomName);
                            broadcast(currentRoom, "방 이름이 '" + newRoomName + "'로 변경되었습니다.");
                        } else {
                            out.println("먼저 채팅방에 입장해주세요.");
                        }
                    } else {
                        out.println("새로운 방 이름을 입력해주세요. 예) /renameRoom [새 방 이름]");
                    }
                } else if ("/exit".equalsIgnoreCase(line.trim())) {
                    if (currentRoom != -1) {
                        leaveRoom(currentRoom, nickname);
                        currentRoom = -1;
                    } else {
                        out.println("방에 먼저 입장해주세요.");
                    }
                } else if ("/bye".equalsIgnoreCase(line.trim())) {
                    leaveRoom(currentRoom, nickname);
                    out.println("채팅 앱을 종료하셨습니다.");
                    socket.close();
                    out.close();
                    in.close();
                    break;

                }else if ((line.indexOf("/whisper")) == 0) {
                    sendMsg(line);

                }else if ("/help".equalsIgnoreCase(line.trim())) {
                    out.println(manualList);

                } else if (line.startsWith("/")) {
                    out.println(wrongMsg);
                    out.println(startMsg);
                }else {
                    if (currentRoom != -1) {
                        broadcast(currentRoom, nickname + ": " + line);
                    } else {
                        out.println("먼저 채팅방에 입장해주세요.");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean chackNickname(String nickname) {
        return ChatServer.nicknameMemory.contains(nickname);
    }
    private synchronized void addNickname(String nickname) {
        ChatServer.nicknameMemory.add(nickname);
    }
    private synchronized int createRoom() {
        int roomNumber = roomCounter++;
        ChatServer.chatRooms.put(roomNumber, new HashMap<>());
        ChatServer.roomClients.put(roomNumber, new ArrayList<>());
        ChatServer.roomNames.put(roomNumber, "Room " + roomNumber);
        System.out.println(roomNumber + " 번 방이 생성되었습니다.");
        return roomNumber;
    }

    private void listRooms() {
        if (ChatServer.chatRooms == null) {
            out.println("현재 생성된 방이 없습니다.");
        }
        out.println("현재 방 목록:");
        for (int roomNumber : ChatServer.chatRooms.keySet()) {
            out.println(roomNumber + " 번 방 (" + ChatServer.roomNames.get(roomNumber) + ")");
        }
    }

    private synchronized void joinRoom(int roomNumber, String clientNickname, PrintWriter clientWriter) {
        if (ChatServer.chatRooms.containsKey(roomNumber)) {
            ChatServer.chatRooms.get(roomNumber).put(clientNickname, clientWriter);
            ChatServer.roomClients.get(roomNumber).add(clientNickname);
            broadcast(roomNumber, clientNickname + "님이 "+  ChatServer.roomNames.get(roomNumber) +" 방에 입장했습니다.");
        } else {
            System.out.println(roomNumber + " 번 방이 존재하지 않습니다.");
        }
    }
    private synchronized void leaveRoom(int roomNumber, String clientNickname) {
        if (currentRoom != -1) {
            if (ChatServer.chatRooms.containsKey(roomNumber)) {
                ChatServer.chatRooms.get(roomNumber).remove(clientNickname);
                ChatServer.roomClients.get(roomNumber).remove(clientNickname);
                broadcast(roomNumber, clientNickname + "님이 방을 나갔습니다.");
                out.println(roomNumber+"번 방을 나갔습니다.");

                if (ChatServer.roomClients.get(roomNumber).isEmpty()) {
                    ChatServer.chatRooms.remove(roomNumber);
                    ChatServer.roomClients.remove(roomNumber);
                    out.println(clientNickname + "님이 마지막으로 나가게되어 " + roomNumber + "번 방이 삭제되었습니다.");
                    roomCounter --;
                }
            }
        }else {
            out.println("현재 방에 입장하지 않으셨습니다.");
        }
    }
    private void sendMsg(String line){
        int firstSpaceIndex = line.indexOf(" ");
        if ((firstSpaceIndex) == -1)
            out.println("/whisper [닉네임] [메시지] 형식으로 입력해주세요.");

        int secondSpaceIndex = line.indexOf(" ", firstSpaceIndex +1 );
        if ((secondSpaceIndex) == -1)
            out.println("/whisper [닉네임] [메시지] 형식으로 입력해주세요.");

        String targetNickname = line.substring(firstSpaceIndex+1,secondSpaceIndex);
        String message = line.substring(secondSpaceIndex+1);

        PrintWriter targetWriter = ChatServer.chatRooms.get(currentRoom).get(targetNickname);
        if (targetWriter != null) {
            targetWriter.println(nickname + "님의 귓속말: " + message);
        } else {
            out.println(targetNickname + " 님이 현재 방에 없습니다.");
        }
    }
    private void broadcast(int roomNumber, String message) {
        Map<String, PrintWriter> clients = ChatServer.chatRooms.get(roomNumber);
        for (PrintWriter writer : clients.values()) {
            writer.println(message);
        }
    }
}
