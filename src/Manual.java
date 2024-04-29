package day18;


public class Manual {
    private String list;
    private String command;

    public Manual(String list, String command) {
        this.list = list;
        this.command = command;
    }

    @Override
    public String toString() {
        return list + " : " + command +"\n";
    }
}
