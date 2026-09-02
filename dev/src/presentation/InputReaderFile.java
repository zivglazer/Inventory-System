package presentation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class InputReaderFile implements InputReader {

    private final Stack<String> lines;

    public InputReaderFile(String filePath) throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(filePath));
        lines = new Stack<>();
        for (int i = allLines.size() - 1; i >= 0; i--) {
            String line = allLines.get(i);
            if (!line.startsWith("/")) {
                lines.push(line.trim());
            }
        }
    }

    @Override
    public String readString() {
        if (lines.isEmpty()) return "";
        return lines.pop();
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
