package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    public String fileName;
    private byte[] content;

    public Blob(File file) {
        this.fileName = file.getName();
        this.content = Utils.readContents(file);
    }

    public String getBlobID() {
        return Utils.sha1((Object) content);
    }

    public byte[] getContent() {
        return content;
    }

    public String getFileName() {
        return fileName;
    }

    public void save() {
        File outFile = Utils.join(Repository.OBJECTS_DIR, getBlobID());
        if (!outFile.exists()) {
            Utils.writeObject(outFile, this);
        }
    }
    public byte[] getBytes() {
        return content;
    }
}