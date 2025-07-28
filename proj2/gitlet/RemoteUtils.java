package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;
import static gitlet.Help.getHeadCommitId;

/**
 * @Author Shiyu
 * @Description
 * 对于远程仓库（如我们整个学期一直使用的skeleton），
 * 我们将简单地使用其他Gitlet仓库。
 * 推送只是将远程仓库尚未拥有的所有提交和blob复制到远程仓库，并重置分支引用。
 * 拉取是相同的，但方向相反。
 */
public class RemoteUtils {
    static TreeMap<String, String> remoteLocationMap = new TreeMap<>();

    static {
        if (remoteRefsInitialized()) {
            remoteLocationMap = readObject(REMOTE_FILE, TreeMap.class);
        }
    }

    public static void saveRemoteLocationMap() {
        writeObject(REMOTE_FILE, remoteLocationMap);
    }

    public static boolean remoteRefsInitialized() {
        return REMOTE_FILE.exists();
    }

    /**
     * ../remote/.gitlet，是的，你将获得远程.gitlet路径
     */
    public static String getRemotePath(String remoteName) {
        return remoteLocationMap.get(remoteName);
    }

    public static File getRemoteGitletFolder(String remoteName) {
        return Utils.join(getRemotePath(remoteName));
    }

    public static boolean isRemoteAdded(String remoteName) {
        return remoteLocationMap.containsKey(remoteName);
    }


    public static File remoteCommitsFolder(String remoteName) {
        return join(getRemoteGitletFolder(remoteName), "commits");
    }

    public static File remoteBranchesFolder(String remoteName) {
        return join(getRemoteGitletFolder(remoteName), "branches");
    }

    public static File remoteObjectsFolder(String remoteName) {
        return join(getRemoteGitletFolder(remoteName), "objects");
    }

    public static void copyCommitFileToRemote(String commitId, String remoteName) {
        if (!isRemoteAdded(remoteName)) {
            return;
        }
        File remoteCommitsFolder = remoteCommitsFolder(remoteName);
        File remoteCommitFile = join(remoteCommitsFolder, commitId);
        writeObject(remoteCommitFile, CommitUtils.readCommit(commitId));
    }

    public static void copyCommitFileFromRemote(String commitId, String remoteName) {
        File remoteCommitsFolder = remoteCommitsFolder(remoteName);
        File remoteCommitFile = join(remoteCommitsFolder, commitId);
        Commit commit = readObject(remoteCommitFile, Commit.class);
        CommitUtils.saveCommit(commit);
    }

    public static void copyBranchFileToRemote(String branchName, String remoteName) {
        if (!isRemoteAdded(remoteName)) {
            return;
        }
        if (!BranchUtils.branchExists(branchName)) {
            return;
        }
        String branchContent = readContentsAsString(join(BRANCHES_DIR, branchName));
        File remoteBranchesFolder = remoteBranchesFolder(remoteName);
        File remoteBrancheFile = join(remoteBranchesFolder, branchName);
        writeContents(remoteBrancheFile, branchContent);
    }

    public static void copyObjectsFileToRemote(String fileSHA1, String remoteName) {
        String fileContent = FileUtils.getFileContent(fileSHA1);
        File remoteObjectsFolder = remoteObjectsFolder(remoteName);
        File remoteObjectFile = join(remoteObjectsFolder, fileSHA1);
        writeContents(remoteObjectFile, fileContent);
    }

    public static void copyObjectsFileFromRemote(String fileSHA1, String remoteName) {
        File remoteObjectsFolder = remoteObjectsFolder(remoteName);
        File remoteObjectFile = join(remoteObjectsFolder, fileSHA1);
        String fileContent = readContentsAsString(remoteObjectFile);
        FileUtils.writeGitletObjectsFile(fileContent);
    }

    public static String readRemoteHEAD(String remoteName) {
        return readContentsAsString(join(getRemotePath(remoteName), "HEAD"));
    }

    public static void writeRemoteHEAD(String remoteName, String content) {
        writeContents(join(getRemotePath(remoteName), "HEAD"), content);
    }

    public static boolean remoteBranchExists(String branchName, String remoteName) {
        File remoteBranchesFolder = remoteBranchesFolder(remoteName);
        List<String> stringList = plainFilenamesIn(remoteBranchesFolder);
        if (stringList == null) {
            return false;
        }
        return stringList.contains(branchName);
    }

    /***
     * @return 如果commitId为null，则返回null。否则，返回远程仓库中的commit对象
     * */
    public static Commit readRemoteCommit(String commitId, String remoteName) {
        if (commitId == null) {
            return null;
        }
        return readObject(join(remoteCommitsFolder(remoteName), commitId), Commit.class);
    }

    /**
     * 在远程仓库中追溯提交。结果将包括当前提交。
     */
    public static List<Commit> remoteCommitTraceback(String commitId, String remoteName) {
        Commit commit = readRemoteCommit(commitId, remoteName);
        Commit ptr = commit;
        List<Commit> result = new LinkedList<>();
        while (ptr != null) {
            result.add(ptr);
            ptr = readRemoteCommit(ptr.getParentId(), remoteName);
        }
        return result;
    }

    /**
     * 在远程仓库中追溯提交。结果将包括当前提交。
     */
    public static List<String> remoteCommitIdTraceback(String commitId, String remoteName) {
        Commit commit = readRemoteCommit(commitId, remoteName);
        Commit ptr = commit;
        List<String> result = new LinkedList<>();
        while (ptr != null) {
            result.add(CommitUtils.getCommitId(ptr));
            ptr = readRemoteCommit(ptr.getParentId(), remoteName);
        }
        return result;
    }

    /**
     * 如果分支在远程分支中不存在，则返回null
     * @return 分支的提交id
     */
    public static String readRemoteBranch(String branchName, String remoteName) {
        if (!remoteBranchExists(branchName, remoteName)) {
            return null;
        }
        File remoteBranchesFolder = remoteBranchesFolder(remoteName);
        return readContentsAsString(join(remoteBranchesFolder, branchName));
    }

    /**
     * 它做两件事：
     * 1. 将remoteName --> remotePath添加到remoteLocationMap并保存
     * 2. 为此远程仓库创建文件夹
     * remotePath可能是../testing/otherdir/.gitlet
     */
    public static void addRemote(String remoteName, String remotePath) {
        if (!REMOTE_FILE.exists()) {
            try {
                REMOTE_FILE.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("failed to create remote file");
            }
        }
        if (isRemoteAdded(remoteName)) {
            System.out.println("A remote with that name already exists.");
            return;
        }
        String[] split = remotePath.split("/");
        StringBuilder convertedPath = new StringBuilder();
        for (String elem : split) {
            convertedPath.append(elem);
            convertedPath.append(File.separator); 
            // 正确的分隔符，Windows使用\，Linux使用/
        }
        convertedPath.delete(convertedPath.length() - 1, convertedPath.length());
        remoteLocationMap.put(remoteName, String.valueOf(convertedPath));
        saveRemoteLocationMap();
    }

    public static void removeRemote(String remoteName) {
        if (!remoteRefsInitialized()) {
            return;
        }
        if (!isRemoteAdded(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remoteLocationMap.remove(remoteName);
        saveRemoteLocationMap();
    }

    public static void push(String remoteName, String remoteBranchName) {
        if (!getRemoteGitletFolder(remoteName).exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        String remoteHEAD = readRemoteHEAD(remoteName);
        String remoteHEADCommitId = readRemoteBranch(remoteHEAD, remoteName);
        Commit currentCommit = CommitUtils.readCommit(getHeadCommitId());
        if (getHeadCommitId().equals(remoteHEADCommitId)) {
            return;
        }
        // 只需将此问题视为链表，而不是复杂的图
        // 顺序是最新提交（前面）--> 较旧提交 --> 初始提交
        List<String> historyCommitId = CommitUtils.commitIdTraceBack(currentCommit);
        if (!historyCommitId.contains(remoteHEADCommitId)) {
            System.out.println("Please pull down remote changes before pushing.");
            return;
        }
        int remoteIdx = historyCommitId.indexOf(remoteHEADCommitId);
        List<String> commitIdAppending = historyCommitId.subList(0, remoteIdx);
        Collections.reverse(commitIdAppending); 
        // 从远程id的下一个 --> 最新的[不包含远程HEAD提交]
        // 将未来的提交追加到远程分支
        for (String commitId : commitIdAppending) {
            // 1. 复制提交文件
            copyCommitFileToRemote(commitId, remoteName);
            // 2. 复制提交对象
            Commit commit = CommitUtils.readCommit(commitId);
            HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
            for (String fileName : fileVersionMap.keySet()) {
                copyObjectsFileToRemote(fileVersionMap.get(fileName), remoteName);
            }
        }
        // 添加此分支（或覆盖此分支）
        copyBranchFileToRemote(remoteBranchName, remoteName);
        // 设置HEAD指向此分支，注意：HEAD始终指向分支名称！
        writeRemoteHEAD(remoteName, remoteBranchName);
    }

    public static void fetch(String remoteName, String remoteBranchName) {
        if (!getRemoteGitletFolder(remoteName).exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        if (!remoteBranchExists(remoteBranchName, remoteName)) {
            System.out.println("That remote does not have that branch.");
            return;
        }
        // 1. 从远程仓库的给定分支复制所有提交和blob
        String remoteCommitId = readRemoteBranch(remoteBranchName, remoteName);
        List<String> allTracedCommitIds = remoteCommitIdTraceback(remoteCommitId, remoteName);
        // 将这些提交文件复制到本地
        for (String commitId : allTracedCommitIds) {
            copyCommitFileFromRemote(commitId, remoteName);
            // 将blob复制到本地
            Commit commit = readRemoteCommit(commitId, remoteName);
            HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
            for (String fileName : fileVersionMap.keySet()) {
                copyObjectsFileFromRemote(fileVersionMap.get(fileName), remoteName);
            }
        }
        // 在本地仓库中创建名为[远程名称]/[远程分支名称]的新分支
        // 并指向远程头提交
        // 注意：因为Windows不允许文件名中有'/'或'\'，
        // 所以我们将创建一个文件夹，并保存提交。
        BranchUtils.saveCommitId(remoteName + "/" + remoteBranchName, remoteCommitId);
    }

    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        String mergedBranchName = remoteName + "/" + remoteBranchName;
        Repository.merge(mergedBranchName);
    }

}
