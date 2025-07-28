package gitlet;

import java.io.IOException;
import java.util.*;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;
import static gitlet.IndexUtils.indexMap;
import static gitlet.IndexUtils.stagedFileContents;

/**
 * @description Repository的核心功能类，包含初始化、添加、提交、删除等基本操作
 */
public class RepositoryCore {
    /** HEAD指针，此指针指向当前分支名称，而不是明确的提交ID，例如 HEAD == "master" */
    public static String HEAD;

    /**
     * @return boolean: 检查此项目是否已初始化gitlet
     * */
    public static boolean isInitialized() {
        return GITLET_DIR.exists();
    }

    // 如果.gitlet已初始化，我们必须将HEAD设置为适当的分支，例如master分支
    static {
        if (isInitialized()) {
            HEAD = new String(readContents(HEAD_FILE));
        }
    }

    /**
     * @description
     * 1. 初始化仓库并创建.gitlet文件夹
     * 2. 创建空的索引文件用于git add命令
     * 3. 创建commits/....(提交ID/SHA-1)来存储空提交
     * 4. 创建branches/master来存储第一个提交ID，意味着master --> 第一个提交
     * 5. 创建HEAD文件，并在其中存储master，意味着HEAD --> master
     * */
    public static void init() {
        if (isInitialized()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        if (!GITLET_DIR.mkdir()) {
            System.out.println("Fail to create .gitlet folder in this work directory.");
            return;
        }

        // 根据上述逻辑，以下代码只会执行一次
        // 为.gitlet创建关键文件夹和文件
        try {
            INDEX_FILE.createNewFile(); // 最初，索引文件将是空的
            HEAD_FILE.createNewFile();
            STAGED_FILE.createNewFile(); // 暂存文件内容
        } catch (IOException e) {
            throw new RuntimeException("failed to create INDEX file and HEAD file");
        }
        COMMITS_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        BRANCHES_DIR.mkdir();

        // 存储并提交第一个空提交
        Commit initialCommit = CommitUtils.makeEmptyCommit("initial commit");
        String initialCommitID = CommitUtils.saveCommit(initialCommit);

        // 生成master分支 --> 初始提交
        BranchUtils.saveCommitId(MASTER_BRANCH_NAME, initialCommitID);

        // HEAD --> MASTER
        setHEAD(MASTER_BRANCH_NAME);
    }

    /***
     * 在gitlet中，一次只能添加一个文件。
     * 但此函数支持一次添加多个文件
     * @param fileName 要添加的文件名
     */
    public static void add(String fileName) {
        if (!join(CWD, fileName).exists()) {
            System.out.println("File does not exist.");
            return;
        }

        // 也许我们应该只更新索引；因为每次提交后，索引和提交映射是相同的；
        if (indexMap.containsKey(fileName)) {
            String targetSHA1 = indexMap.get(fileName);
            // 如果文件与当前索引文件相同，意味着没有变化，则直接返回
            if (FileUtils.hasSameSHA1(fileName, targetSHA1)) return;
        }

        IndexUtils.stageFile(fileName);
        IndexUtils.saveIndex();
    }

    /***
     * 对于跟踪的文件从工作目录中丢失或在工作目录中更改，这不是失败
     * 建议实现：在当前提交和暂存区域中保存[跟踪文件的快照]，以便稍后可以恢复
     * 注意：这是sha-1的魔力：如果提交1和3有相同的test.txt，那么提交3只会覆盖test.txt对象文件（相同的sha1）
     * @param commitMessage 每个提交必须包含提交消息
     */
    public static void commit(String commitMessage) {
        if (commitMessage.isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }
        String currentCommitId = getHeadCommitId();
        Commit currentCommit = CommitUtils.readCommit(currentCommitId);
        HashMap<String, String> fileVersionMap = currentCommit.getFileVersionMap();
        // bug：fileVersionMap可能为null，但indexMap永远不会为null（在git init之后）
        if (indexMap.equals(fileVersionMap)) {
            // 注意：此实现与proj2文档不同
            System.out.println("No changes added to the commit.");
        }
        Commit newCommit = CommitUtils.makeCommit(commitMessage, currentCommitId, indexMap);
        CommitUtils.createFileObjects(currentCommit, newCommit, stagedFileContents); // 创建文件（与上次提交不同）
        stagedFileContents.clear();
        IndexUtils.saveIndex(); // 清除并保存
        String newCommitId = CommitUtils.saveCommit(newCommit);
        BranchUtils.saveCommitId(HEAD, newCommitId); // 保存当前分支指针 --> 新提交ID
    }

    /***
     * 逻辑只会删除工作目录中的明确文件，而不是.gitlet中的文件
     * @param fileName 工作目录中的文件名
     */
    public static void rm(String fileName) {
        Commit commit = CommitUtils.readCommit(getHeadCommitId());
        boolean staged = IndexUtils.isStaged(fileName, commit);
        boolean trackedByHeadCommit = CommitUtils.isTrackedByCommit(commit, fileName);
        if (!staged && !trackedByHeadCommit) {
            System.out.println("No reason to remove the file.");
            return;
        }
        IndexUtils.unstageFile(fileName);
        IndexUtils.saveIndex(); // 注意：所有更改都必须保存
        if (trackedByHeadCommit) {
            // 如果它被当前提交跟踪，你应该删除CWD中的文件
            restrictedDelete(join(CWD, fileName));
        }
    }

    /**
     * 它设置HEAD --> branch_name（其他函数可能是关于在提交上设置head，
     * 但此项目将忽略这种情况）
     * 同时，它保存HEAD文件
     * @param branchName 参数必须存在，否则将抛出AssertionError
     * */
    public static void setHEAD(String branchName) {
        assert BranchUtils.branchExists(branchName);
        HEAD = branchName;
        writeContents(HEAD_FILE, branchName);
    }

    /***
     * head --> 分支名称 --> 提交ID
     */
    public static String getHeadCommitId() {
        return BranchUtils.getCommitId(HEAD);
    }
} 