package gitlet;

import java.io.IOException;
import java.util.*;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;
import static gitlet.IndexUtils.indexMap;
import static gitlet.IndexUtils.stagedFileContents;

/**
 * @description 表示一个gitlet仓库。提供被Main方法调用的辅助函数。
 * 例如：当输入(git add)时，Main方法将调用此Repository类中的相关辅助方法
 */
public class Repository {
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

    /***
     * 从头提交到初始提交跟踪提交链
     */
    public static void log() {
        Commit currentCommit = CommitUtils.readCommit(getHeadCommitId());
        List<Commit> commits = CommitUtils.commitTraceBack(currentCommit);
        for (Commit commit : commits) {
            commit.printCommitInfo();
        }
    }

    /***
     * global-log：以随机顺序打印所有提交
     */
    public static void globalLog() {
        List<String> commitIdList = plainFilenamesIn(COMMITS_DIR);
        if (commitIdList == null || commitIdList.isEmpty()) {
            return;
        }
        for (String commitId : commitIdList) {
            CommitUtils.readCommit(commitId).printCommitInfo();
        }
    }

    /**
     * 打印出具有给定提交消息的所有提交的ID，每行一个
     */
    public static void find(String commitMessage) {
        List<String> commitIdList = plainFilenamesIn(COMMITS_DIR);
        if (commitIdList == null || commitIdList.isEmpty()) {
            return;
        }
        boolean printFlag = false;
        for (String commitId : commitIdList) {
            Commit commit = CommitUtils.readCommit(commitId);
            if (commitMessage.equals(commit.getMessage())) {
                System.out.println(CommitUtils.getCommitId(commit));
                printFlag = true;
            }
        }
        if (!printFlag) {
            System.out.println("Found no commit with that message.");
        }
    }

    /***
     * 只是一个checkout接口，此命令将通过其调用的不同方法之一完成
     * @param args 来自命令行的其余参数
     */
    public static void checkout(String...args) {
        Commit commit = null;
        if (args.length > 1) {
            String fileName;
            if (args.length == 2) {
                if (!args[0].equals("--")) {
                    System.out.println("Incorrect operands.");
                }
                fileName = args[1];
                commit = CommitUtils.readCommit(getHeadCommitId());
            } else {
                if (!args[1].equals("--")) {
                    System.out.println("Incorrect operands.");
                }
                fileName = args[2];
                commit = CommitUtils.readCommitByPrefix(args[0]);
                if (commit == null) {
                    System.out.println("No commit with that id exists.");
                    return;
                }
            }
            checkoutFile(commit, fileName);
        } else {
            commit = CommitUtils.readCommit(getHeadCommitId());
            checkoutBranch(commit, args[0]);
        }
    }

    /**
     * 更改为新分支的指针提交，就像新分支的提交刚刚发生一样。
     * 所以indexMap(& .gitlet/index)与新分支提交fileVersionMap相同，stagedFiles(& .gitlet/staged-files)被清除。
     * @param commit 当前提交对象（分支更改前）
     * @param branchName 要更改为的分支名称
     */
    private static void checkoutBranch(Commit commit, String branchName) {
        if (!BranchUtils.branchExists(branchName)) { // branchExists()将断言branchName != null
            System.out.println("No such branch exists.");
            return;
        }
        if (branchName.equals(HEAD)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        assert CWDFileNames != null;
        for (String fileName : CWDFileNames) {
            if (!CommitUtils.isTrackedByCommit(commit, fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }
        // 将提交恢复到CWD
        Commit newBranchCommit = CommitUtils.readCommit(BranchUtils.getCommitId(branchName));
        restoreCommit(newBranchCommit);

        // 3. 设置HEAD == 新分支名称
        setHEAD(branchName);
    }

    /**
     * 将此提交恢复到CWD，并恢复索引区域（清除stagedFileContents并恢复indexMap）
     * 就像提交刚刚发生一样。
     */
    private static void restoreCommit(Commit commit) {
        Commit currentCommit = CommitUtils.readCommit(getHeadCommitId());
        // 预检查
        for (String fileName : commit.getFileVersionMap().keySet()) {
            if (FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) {
                System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                return;
            }
        }

        // 1. 将文件恢复到CWD
        FileUtils.restoreCommitFiles(commit);

        // 2. 恢复indexMap
        // 注意：为了保持一致性，checkout分支就像新分支的commit()刚刚发生一样
        // 所以它将恢复indexMap & .gitlet/index，但stagedFiles及其文件保持为空。
        indexMap = commit.getFileVersionMap();
        stagedFileContents.clear();
        IndexUtils.saveIndex();
    }

    /***
     * 提示：文件的新版本未暂存。这意味着我们不应该更改暂存区域
     */
    public static void checkoutFile(Commit commit, String fileName) {
        if (!CommitUtils.isTrackedByCommit(commit, fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String fileSHA1 = commit.getFileVersionMap().get(fileName);
        String fileContent = FileUtils.getFileContent(fileSHA1);
        FileUtils.writeCWDFile(fileName, fileContent);
    }

    /***
     * 使用给定名称创建新分支，并将其指向当前头提交。
     * @param branchName 你创建的新分支名称
     */
    public static void branch(String branchName) {
        if (BranchUtils.branchExists(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        BranchUtils.saveCommitId(branchName, getHeadCommitId());
    }

    /**
     * 删除branchName的分支文件
     */
    public static void removeBranch(String branchName) {
        if (!BranchUtils.branchExists(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (HEAD.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        BranchUtils.removeBranch(branchName);
    }

    /**
     * 打印出一些状态消息
     */
    public static void status() {
        // 打印分支
        List<String> allBranchNames = BranchUtils.getAllBranchNames();
        System.out.println("=== Branches ===");
        for (String branchName : allBranchNames) {
            System.out.println((HEAD.equals(branchName) ? "*" : "") + branchName);
        }
        System.out.println();

        // 打印暂存文件
        Commit commit = CommitUtils.readCommit(getHeadCommitId());
        List<String> stagedFileNames = IndexUtils.getStagedFiles(commit);
        System.out.println("=== Staged Files ===");
        stagedFileNames.forEach(System.out::println);
        System.out.println();

        // 打印已删除文件
        List<String> removedFileNames = IndexUtils.getRemovedFiles(commit);
        System.out.println("=== Removed Files ===");
        removedFileNames.forEach(System.out::println);
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        List<StringBuffer> modifiedNotStagedForCommit = IndexUtils.modifiedNotStagedForCommit(commit);
        List<StringBuffer> deletedNotStagedForCommit = IndexUtils.deletedNotStagedForCommit(commit);
        modifiedNotStagedForCommit.forEach(s -> s.append(" (modified)"));
        deletedNotStagedForCommit.forEach(s -> s.append(" (deleted)"));
        modifiedNotStagedForCommit.addAll(deletedNotStagedForCommit);
        modifiedNotStagedForCommit.sort(StringBuffer::compareTo);
        modifiedNotStagedForCommit.forEach(System.out::println);
        System.out.println();

        // （"未跟踪文件"）是指存在于工作目录中但既未暂存添加也未跟踪的文件
        System.out.println("=== Untracked Files ===");
        List<String> untrackedFileNames = IndexUtils.getUntrackedFiles(commit);
        untrackedFileNames.forEach(System.out::println);
        System.out.println();
    }

    /**
     * 该命令本质上是任意提交的checkout，它也会更改当前分支头。
     */
    public static void reset(String commitIdPrefix) {
        Commit commit = CommitUtils.readCommitByPrefix(commitIdPrefix);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String commitId = CommitUtils.getCommitId(commit);
        restoreCommit(commit);
        BranchUtils.saveCommitId(HEAD, commitId);
    }

    /**
     * @note
     * 如果当前提交中的未跟踪文件将被合并覆盖或删除，打印"There is an
     * untracked file in the way; delete it, or add and commit it first."并退出；在执行其他任何操作之前执行此检查。
     */
    public static void merge(String branchName) {
        // 预检查失败情况
        if (!BranchUtils.branchExists(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (HEAD.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Commit currentCommit = CommitUtils.readCommit(getHeadCommitId());
        List<String> stagedFileNames = IndexUtils.getStagedFiles(currentCommit);
        List<String> removedFileNames = IndexUtils.getRemovedFiles(currentCommit);
        if (!stagedFileNames.isEmpty() || !removedFileNames.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        // 获取当前分支提交、目标分支提交和分割点提交
        Commit branchCommit = CommitUtils.readCommit(BranchUtils.getCommitId(branchName));
        // Commit splitPoint = CommitUtils.getSplitCommit(HEAD, branchName);
        Commit splitPoint = CommitUtils.getSplitCommitWithGraph(HEAD, branchName);

        // 当前分支和目标分支在同一行的情况
        if (splitPoint == null || CommitUtils.isSameCommit(branchCommit, splitPoint)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return; // 在这种情况下，head和branch指向同一个提交，无需合并
        }
        if (CommitUtils.isSameCommit(currentCommit, splitPoint)) {
            String savedHEAD = HEAD;
            checkout(branchName); // checkout分支，注意它会改变head --> 另一个分支
            HEAD = savedHEAD;
            // 快进master指针
            BranchUtils.saveCommitId(HEAD, BranchUtils.getCommitId(branchName));
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        // 复杂情况：无冲突合并或有冲突合并
        Set<String> splitPointFiles = splitPoint.getFileVersionMap().keySet();
        Set<String> currentCommitFiles = currentCommit.getFileVersionMap().keySet();
        Set<String> branchCommitFiles = branchCommit.getFileVersionMap().keySet();
        // 合并上述三个集合以获得三个提交中的所有相关文件
        // bug：你必须分配新内存，而不是引用
        Set<String> allRelevantFiles = new HashSet<>(splitPointFiles); // 变量splitPointFiles有其他用途
        allRelevantFiles.addAll(currentCommitFiles);
        allRelevantFiles.addAll(branchCommitFiles);

        boolean conflictFlag = false;

        for (String fileName : allRelevantFiles) {
            boolean splitCurrentConsistent = CommitUtils.isConsistent(fileName, splitPoint, currentCommit);
            boolean splitBranchConsistent = CommitUtils.isConsistent(fileName, splitPoint, branchCommit);
            boolean branchCurrentConsistent = CommitUtils.isConsistent(fileName, currentCommit, branchCommit);
            // 无冲突合并
            if ((splitBranchConsistent && !splitCurrentConsistent) || branchCurrentConsistent) {
                continue;
            }

            if (!splitBranchConsistent && splitCurrentConsistent) {
                if (!branchCommitFiles.contains(fileName)) {
                    // 在这种情况下，其他两个提交必须包含该文件
                    // 从CWD中删除文件，不在合并提交中跟踪此文件
                    // 这意味着删除indexMap中此fileName的记录
                    if (FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) { // 需要安全检查
                        System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                        return;
                    } else {
                        rm(fileName);
                    }
                } else {
                    // 在这种情况下，我们将checkout branchCommit中的文件并将其添加到索引
                    if (FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) { // 需要安全检查
                        System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                        return;
                    } else {
                        checkoutFile(branchCommit, fileName);
                        add(fileName);
                    }
                }
                continue;
            }

            // 有冲突合并，如果逻辑可以简化
            if (!splitBranchConsistent && !splitCurrentConsistent && !branchCurrentConsistent) {
                conflictFlag = true;
                StringBuilder conflictedContents = new StringBuilder("<<<<<<< HEAD\n");
                String currentCommitContent =  currentCommitFiles.contains(fileName) ?
                                               FileUtils.getFileContent(fileName, currentCommit) : "";
                String branchCommitContent = branchCommitFiles.contains(fileName) ?
                                             FileUtils.getFileContent(fileName, branchCommit) : "";
                conflictedContents.append(currentCommitContent);
                conflictedContents.append("=======\n");
                conflictedContents.append(branchCommitContent);
                conflictedContents.append(">>>>>>>\n");
                if (FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) { // 需要安全检查
                    System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                    return;
                } else {
                    FileUtils.writeCWDFile(fileName, String.valueOf(conflictedContents));
                    add(fileName);
                }
            }
        }

        // 1. 创建提交 2. 设置此新提交的secondParentId
        commit("Merged " + branchName + " into " + HEAD + ".");
        Commit mergeCommit = CommitUtils.readCommit(getHeadCommitId());
        mergeCommit.setSecondParentId(BranchUtils.getCommitId(branchName));
        // bug：你必须保存合并提交。所有更改都必须保存
        CommitUtils.saveCommit(mergeCommit);

        // 3. 其他要做的事情：你必须让当前分支 --> 合并提交
        BranchUtils.saveCommitId(HEAD, CommitUtils.getCommitId(mergeCommit));

        // 如果有冲突，你应该输出一些消息
        if (conflictFlag) {
            System.out.println("Encountered a merge conflict.");
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
