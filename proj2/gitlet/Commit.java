package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 关于transient的解释：
 * 当你将对象写入文件时，它会将自身和所有引用的成员都写入
 * 不要在运行时对象中使用Java指针来引用提交和blob，而是
 * 使用SHA-1哈希字符串。维护一个运行时映射（永远不写入文件）在这些SHA-1
 * 字符串和它们引用的运行时对象之间。（我认为这个运行时映射应该存储
 * 在Repository中，而不是在这个类中）
 * @description 此类只是一个JavaBean，用于存储关键信息。
 * 不用于执行任何操作。
 * 此类将被序列化到[.gitlet]中[commits]文件夹的文件中
 */
public class Commit implements Serializable {
    /** 此提交的消息 */
    private String message;
    /** 提交时间戳 */
    private Date commitTime;

    /** 父提交SHA1值 */
    private String parentId;

    /** 第二个父提交 */
    private String secondParentId;

    /** 存储文件名和其版本（由SHA-1表示） */
    private HashMap<String, String> fileVersionMap;

    /***
     * fileVersionMap永远不会为null
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

    public void setParentId(String ParentId) {
        this.parentId = ParentId;
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
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        System.out.println("===");
        System.out.println("commit " + CommitUtils.getCommitId(this));
        if (secondParentId != null) {
            System.out.println("Merge: " + parentId.substring(0, 7) + " " + secondParentId.substring(0, 7));
        }
        System.out.println("Date: " + sdf.format(this.commitTime));
        System.out.println(this.message);
        System.out.println();
    }
}
