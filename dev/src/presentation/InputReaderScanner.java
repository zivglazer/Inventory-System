package presentation;

import java.util.Scanner;

public class InputReaderScanner implements InputReader {

    private final Scanner scanner;

    public InputReaderScanner() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public String readString() {
        return scanner.nextLine().trim();
    }

    @Override
    public int readInt() {
        try {
            return Integer.parseInt(readString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
