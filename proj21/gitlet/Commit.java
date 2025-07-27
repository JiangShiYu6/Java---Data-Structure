package gitlet;
// TODO: any imports you need here
import java.io.Serializable;
import java.util.*;

/**
 * Represents a Gitlet commit object.
 *
 * A commit in Gitlet represents a snapshot of the project at a given point in time.
 * It contains metadata such as a message, timestamp, parent commit ID, and a map
 * from file names to blob IDs (UIDs) representing the state of tracked files.
 *
 * In Gitlet, all commits are stored as serialized files under `.gitlet/objects/`,
 * where the filename is the SHA-1 UID of the commit's serialized content.
 *
 * The initial commit has no parent and uses a fixed timestamp of 00:00:00 UTC, Jan 1, 1970.
 *
 * @author TODO
 */
public class Commit implements Serializable {

    /** The commit message describing this snapshot (e.g., "initial commit", "Fixed bug", etc.). */
    private String message;

    /** The timestamp of when this commit was created. Initial commit uses Unix epoch (0). */
    private Date timeStamp;

    /** The UID (SHA-1 hash) of the parent commit. Null if this is the initial commit. */
    private List<String> parents;

    /**
     * A map representing tracked files at this commit.
     * Key: file name (e.g., "foo.txt")
     * Value: blob UID (SHA-1 hash of file contents)
     */
    private HashMap<String, String> blobs;// 文件名 → blobID 的映射

    private String id;

    /** Constructs the initial commit with no parent and fixed timestamp. */
    public Commit() {
        this.message = "initial commit";
        this.timeStamp = new Date(0); // Unix Epoch
        this.parents = new ArrayList<>();  // 空列表，表示没有父 commit
        this.blobs = new HashMap<>();
        this.id = null; // 临时设置为null
        this.id = Utils.sha1(Utils.serialize(this));
    }
    public Commit(String message, List<String> parentID, Map<String, String> blobs) {
        this.message = message == null ? "" : message;
        this.parents = parentID == null ? new ArrayList<>() : new ArrayList<>(parentID);
        this.blobs = blobs == null ? new HashMap<>() : new HashMap<>(blobs);
        this.timeStamp = new Date();
        this.id = null; // 临时设置为null
        this.id = Utils.sha1(Utils.serialize(this));
    }
    //文件名,blobID
    public Map<String, String> getBlobs() {
        return blobs;
    }
    //生成当前 Commit 对象的唯一标识符（即 SHA-1 哈希值）。
    public String getId() {
        return id;
    }
    public Date getTimestamp(){
        return timeStamp;
    }
    public String getMessage(){
        return message;
    }
    public List<String> getParents(){
        return parents;
    }



}
