package AcLuaDev;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

public class DirectoryNode {
    String name;
    final ArrayList<DirectoryNode> childDirs = new ArrayList<>();
    final HashMap<String, RandomAccessFile> files = new HashMap<>();

    public DirectoryNode(String name) {
        this.name = name;
    }
}
