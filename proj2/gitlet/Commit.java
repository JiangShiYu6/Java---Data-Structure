package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Interpret transient:
 * when you write an object to file, it will write itself and all members it references to
 * Don’t use Java pointers to refer to commits and blobs in your runtime objects, but instead
 * use SHA-1 hash strings. Maintain a runtime map(never write to file) between these SHA-1
 * strings and the runtime objects they refer to. (I think this runtime map should be stored
 * in Repository, not in this class)
 * @description this class is just a JavaBean which stores critical information.
 * Not for doing anything.
 * this class will be serialized to a file in [commits] folder in [.gitlet]
 */
public class Commit implements Serializable {
    /** 此提交的消息 */
    private String message;
    /** 提交时间戳 */
    private Date commitTime;

    /** 父提交的SHA1值 */
    private String parentId;

    /** 第二个父提交 */
    private String secondParentId;

    /** 存储文件名和其版本（用SHA-1表示） */
    private HashMap<String, String> fileVersionMap;

    /***
     * fileVersionMap永远不会为null。
     */
    public Commit() {
        fileVersionMap = new HashMap<>();
    }

    public String getMessage() {
        return message;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public String getParentId() {
        return parentId;
    }

    public String getSecondParentId() {
        return secondParentId;
    }

    public HashMap<String, String> getFileVersionMap() {
        return fileVersionMap;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setSecondParentId(String secondParentId) {
        this.secondParentId = secondParentId;
    }

    public void setFileVersionMap(HashMap<String, String> fileVersionMap) {
        this.fileVersionMap = fileVersionMap;
    }

    /***
     * 为log命令打印关键信息
     */
    public void printCommitInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat(
            "EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        System.out.println("===");
        System.out.println("commit " + CommitUtils.getCommitId(this));
        if (secondParentId != null) {
            System.out.println("Merge: " + parentId.substring(0, 7) 
                + " " + secondParentId.substring(0, 7));
        }
        System.out.println("Date: " + sdf.format(this.commitTime));
        System.out.println(this.message);
        System.out.println();
    }
}
