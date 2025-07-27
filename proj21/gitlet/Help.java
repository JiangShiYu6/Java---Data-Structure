package gitlet;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.swing.text.Utilities;

import static gitlet.Repository.*;
import static gitlet.Utils.join;
import static gitlet.Utils.sha1;

public class Help {
    /**
     * 检查是否初始化
     */
    public static void checkIfInit() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
    //检查文件是否存在
    public static File checkFileExists(String fileName) {
        File cwd = new File(System.getProperty("user.dir"));
        File file = new File(cwd, fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        return file;
    }
    /**
     * 返回当前分支对应的分支指针文件（如 .gitlet/refs/heads/master）
     */
    public static File getCurrentBranchFile() {
        String headRef = Utils.readContentsAsString(HEAD_FILE).trim();
        String branchName;
        String prefix = "refs/heads/";
        if (headRef.startsWith(prefix)) {
            branchName = headRef.substring(prefix.length());
        } else {
            // 兼容直接存分支名的情况
            branchName = headRef;
        }
        return Utils.join(HEADS_DIR, branchName);
    }


    //获取当前分支对应的 commit 对象
    public static Commit getCurrentCommit() {
        String headRef = Utils.readContentsAsString(HEAD_FILE).trim();
        String branchName;
        String prefix = "refs/heads/";
        if (headRef.startsWith(prefix)) {
            branchName = headRef.substring(prefix.length());
        } else {
            // 兼容直接存分支名的情况
            branchName = headRef;
        }
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("Branch file does not exist: " + branchFile.getPath());
            return null;
        }
        String commitID = Utils.readContentsAsString(branchFile).trim();
        if (commitID.isEmpty()) {
            System.out.println("Commit ID is empty for branch: " + branchName);
            return null;
        }
        File commitFile = join(OBJECTS_DIR, commitID);
        if (!commitFile.exists()) {
            System.out.println("Commit file does not exist: " + commitFile.getPath());
            return null;
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    /**
     * 返回由多个字符串或字节数组拼接后生成的 SHA-1 哈希值。
     * 用于生成 blob 或 commit 的唯一标识符。
     */
    public static String computeHash(Object... inputs) {
        return sha1(inputs);  // 复用 Utils 类已有的 sha1 方法
    }
    //写入 blob 内容（如果尚未存在）
    public static void writeBlobIfNotExists(String blobHash, byte[] content) {
        File blobFile = join(OBJECTS_DIR, blobHash);
        if (!blobFile.exists()) {
            Utils.writeContents(blobFile, content);
        }
    }
    public static String expandAbbreviatedID(String shortID) {
        File[] files = OBJECTS_DIR.listFiles();
        if (files == null) {
            return null;
        }

        String match = null;
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(shortID)) {
                if (match != null) {
                    // 已经匹配到一个，发现第二个冲突 → 不唯一
                    System.out.println("Multiple commits match abbreviated ID.");
                    System.exit(0);
                }
                match = name;
            }
        }
        return match;
    }
    public static String findSplitPoint(Commit currentCommit, Commit givenCommit) {
        // 用于记录 currentCommit 所有祖先的 commit ID
        Set<String> ancestorsOfCurrent = new HashSet<>();
        // BFS 遍历 currentCommit 的祖先
        Queue<Commit> queue = new LinkedList<>();
        queue.add(currentCommit);
        while (!queue.isEmpty()) {
            Commit commit = queue.poll();
            String id = commit.getId(); // 使用 commit 的 getId() 方法
            ancestorsOfCurrent.add(id);
            List<String> parents = commit.getParents();
            if (parents != null) {
                for (String parentID : parents) {
                    Commit parentCommit = readCommitByID(parentID);
                    if (parentCommit != null) {
                        queue.add(parentCommit);
                    }
                }
            }
        }
        // BFS 遍历 givenCommit，找第一个在 ancestorsOfCurrent 中出现的
        Queue<Commit> queue2 = new LinkedList<>();
        queue2.add(givenCommit);
        while (!queue2.isEmpty()) {
            Commit commit = queue2.poll();
            String id = commit.getId(); // 使用 commit 的 getId() 方法
            if (ancestorsOfCurrent.contains(id)) {
                return id; // 找到 split point
            }
            List<String> parents = commit.getParents();
            if (parents != null) {
                for (String parentID : parents) {
                    Commit parentCommit = readCommitByID(parentID);
                    if (parentCommit != null) {
                        queue2.add(parentCommit);
                    }
                }
            }
        }

        return null; // 如果没有共同祖先，返回 null（极端情况）

    }
    public static void checkIfCheckedCurrBranch(String branchName) {
        String currBranch = readCurrBranch();
        if (branchName.equals(currBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
    }
    public static String readCurrBranch() {
        String headRef = Utils.readContentsAsString(HEAD_FILE).trim();
        String prefix = "refs/heads/";
        if (headRef.startsWith(prefix)) {
            return headRef.substring(prefix.length());
        } else {
            // 兼容直接存分支名的情况
            return headRef;
        }
    }
    public static void checkIfCheckedBranchExists(String branchName) {
        List<String> allBranch = readAllBranch();
        if (!allBranch.contains(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
    }
    public static List<String> readAllBranch() {
        return Utils.plainFilenamesIn(HEADS_DIR);
    }
    public static void checkIfCommitIDExists(String commitID) {
        Commit commit = readCommitByID(commitID);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
    }
    
    static Commit readCommitByID(String commitID) {
        if (commitID.length() == 40) {
            File commitFile = Utils.join(OBJECTS_DIR, commitID);
            if (!commitFile.exists()) {
                return null;
            }
            return Utils.readObject(commitFile, Commit.class);
        } else {
            List<String> objectIDs = Utils.plainFilenamesIn(OBJECTS_DIR);
            if (objectIDs == null) {
                return null;
            }
            for (String o : objectIDs) {
                if (o.startsWith(commitID)) {
                    File commitFile = Utils.join(OBJECTS_DIR, o);
                    return Utils.readObject(commitFile, Commit.class);
                }
            }
            return null;
        }
    }
    public static String readCurrCommitID() {
        String currBranch = readCurrBranch();
        File headsFile = Utils.join(HEADS_DIR, currBranch);
        return Utils.readContentsAsString(headsFile);
    }
    public static void changeBranchTo(String headName) {
        Utils.writeContents(HEAD_FILE, headName);
    }
    public static void changeBranchHeadTo(String commitID, String branchName) {
        File branchFile = Utils.join(HEADS_DIR, branchName);
        Utils.writeContents(branchFile, commitID);
    }
    public static Commit readCurrCommit() {
        String currCommitID = readCurrCommitID();
        File CURR_COMMIT_FILE = join(OBJECTS_DIR, currCommitID);
        return Utils.readObject(CURR_COMMIT_FILE, Commit.class);
    }
    public static Blob getBlobByID(String id) {

        File BLOB_FILE = join(OBJECTS_DIR, id);
        return Utils.readObject(BLOB_FILE, Blob.class);
    }

    /**
     * 根据分支名读取对应的commit ID
     */
    public static String readCurrCommitIDByBranch(String branchName) {
        File branchFile = Utils.join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            return null;
        }
        return Utils.readContentsAsString(branchFile);
    }
    public static void changeCommitTo(Commit newCommit) {
        // 更新工作目录
        Map<String, String> newBlobs = newCommit.getBlobs();
        for (Map.Entry<String, String> entry : newBlobs.entrySet()) {
            String fileName = entry.getKey();
            String blobID = entry.getValue();
            File blobFile = Utils.join(OBJECTS_DIR, blobID);
            Blob blob = Utils.readObject(blobFile, Blob.class);
            File workFile = Utils.join(CWD, fileName);
            Utils.writeContents(workFile, blob.getContent());
        }

        // 删除新提交中不存在的文件
        Commit currentCommit = getCurrentCommit();
        Map<String, String> currentBlobs = currentCommit.getBlobs();
        for (String fileName : currentBlobs.keySet()) {
            if (!newBlobs.containsKey(fileName)) {
                File workFile = Utils.join(CWD, fileName);
                if (workFile.exists()) {
                    workFile.delete();
                }
            }
        }

        // 清空暂存区
        Stage stage = new Stage();
        stage.save();
    }
    public static Commit readCommitByBranchName(String branchName) {
        File branchFileName = join(HEADS_DIR, branchName);
        String newCommitID = Utils.readContentsAsString(branchFileName);
        return readCommitByID(newCommitID);
    }
    public static void checkIfBranchExists(String branchName) {
        List<String> allBranch = readAllBranch();
        if (!allBranch.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }
    public static void overwriteFiles(List<String> bothCommitTracked, Commit newCommit) {
        if (bothCommitTracked == null || bothCommitTracked.isEmpty()) {
            return;
        }
        for (String fileName : bothCommitTracked) {
            String blobID = newCommit.getBlobs().get(fileName);
            if (blobID == null) {
                continue;
            }
            Blob blob = getBlobByID(blobID);
            writeBlobToCWD(blob);
        }
    }
    public static void writeBlobToCWD(Blob blob) {
        if (blob == null) {
            return;
        }
        File outFile = new File(System.getProperty("user.dir"), blob.getFileName());
        Utils.writeContents(outFile, blob.getContent());
    }
    


}
