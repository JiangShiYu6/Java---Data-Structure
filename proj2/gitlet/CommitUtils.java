package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;


/**
 * @Author Shiyu
 * @Description 提交工具类，提供提交相关的操作方法
 */
public class CommitUtils {
    /**
     * 创建一个没有文件版本映射的空提交
     * 没有父提交SHA-1和父提交对象（类型也是Commit）
     * @note 必须设置一个空的HashMap以避免空指针异常
     * @param message 提交消息
     * @return 提交Java Bean对象
     * */
    public static Commit makeEmptyCommit(String message) {
        Commit commit = new Commit();
        commit.setMessage(message);
        commit.setCommitTime(new Date(0));
        commit.setParentId(null);
        commit.setSecondParentId(null);
        return commit;
    }

    /***
     * 创建一个普通的提交bean对象
     * @note 必须设置一个空的HashMap以避免空指针异常
     * @param message 提交消息，我们不会检查是否为空
     * @param parentCommitId 显而易见的参数
     * @param fileVersionMap 总是来自当前索引映射，如果为null，将被替换为空的哈希映射
     * @return 提交bean对象
     */
    public static Commit makeCommit(String message,
                                    String parentCommitId, HashMap<String, String> fileVersionMap) {
        Commit commit = new Commit();
        commit.setMessage(message);
        commit.setParentId(parentCommitId);
        commit.setFileVersionMap(fileVersionMap == null ? new HashMap<>() : fileVersionMap);
        commit.setCommitTime(new Date());
        commit.setSecondParentId(null);
        return commit;
    }

    /***
     * 将提交bean保存到.gitlet/commits目录，文件名为[SHA1]，内容是
     * 可序列化的bean字符串
     * @param commit 提交bean对象
     * @return 提交ID（SHA-1）
     */
    public static String saveCommit(Commit commit) {
        // 注意：我们可能使用序列化字符串(byte[])来计算SHA-1（而不是文件）
        // 因为序列化对象是字符串，它将直接写入文件
        String CommitId = getCommitId(commit); // byte[]将被视为一个对象
        File commitFile = join(COMMITS_DIR, CommitId);
        writeObject(commitFile, commit); // 存储我们的第一个提交
        return CommitId;
    }

    /**
     * 获取提交的ID
     */
    public static String getCommitId(Commit commit) {
        return sha1(serialize(commit));
    }

    /***
     * 从提交ID恢复提交Java bean对象
     * @param commitId 提交的sha-1
     */
    public static Commit readCommit(String commitId) {
        if (commitId == null) {
            return null;
        }
        return readObject(join(COMMITS_DIR, commitId), Commit.class);
    }

    /***
     * 通过SHA-1前缀查找正确的提交bean对象
     * @param prefix 提交的sha-1前缀
     * @warning 此函数有bug，例如：前缀冲突
     * @return 如果读取失败，如果没有异常，将返回null
     */
    public static Commit readCommitByPrefix(String prefix) {
        List<String> commitIdList = plainFilenamesIn(COMMITS_DIR);
        if (commitIdList == null) {
            return null;
        }
        int queryCount = 0;
        String resultCommitId = null;
        for (String commitId : commitIdList) {
            if (commitId.startsWith(prefix)) {
                queryCount++;
                resultCommitId = commitId;
            }
        }
        if (queryCount > 1) {
            throw new RuntimeException("this prefix is ambiguous, you must use longer prefix");
        }
        return readCommit(resultCommitId);
    }

    /***
     * 比较旧提交映射和新映射，并在新映射中创建新对象
     * 注意：直接从工作目录保存文件是不安全的，因为用户可能会更改工作目录中文件的内容
     * 相反，我们应该将文件保存在内存中，然后将它们保存到磁盘，以保持（sha1 <-- 正确内容）
     */
    public static void createFileObjects(Commit oldCommit, Commit newCommit, HashMap<String, String> stagedFiles) {
        HashMap<String, String> oldFileVersion = oldCommit.getFileVersionMap();
        HashMap<String, String> newFileVersion = newCommit.getFileVersionMap();
        for (String fileName : newFileVersion.keySet()) {
            if (oldFileVersion.containsKey(fileName)) {
                if (!oldFileVersion.get(fileName).equals(newFileVersion.get(fileName))) {
                    FileUtils.writeGitletObjectsFile(stagedFiles.get(newFileVersion.get(fileName)));
                }
            } else  {
                FileUtils.writeGitletObjectsFile(stagedFiles.get(newFileVersion.get(fileName)));
            }
        }
    }

    /**
     * 检查文件是否被指定提交跟踪
     */
    public static boolean isTrackedByCommit(String commitId, String fileName) {
        Commit commit = readCommit(commitId);
        return isTrackedByCommit(commit, fileName);
    }

    /**
     * 检查文件是否被指定提交跟踪
     */
    public static boolean isTrackedByCommit(Commit commit, String fileName) {
        assert commit != null && fileName != null;
        return commit.getFileVersionMap().containsKey(fileName);
    }

    /**
     * 判断两个提交是否相同
     */
    public static boolean isSameCommit(Commit commit1, Commit commit2) {
        assert commit1 != null && commit2 != null;
        return getCommitId(commit1).equals(getCommitId(commit2));
    }

    /**
     * 回溯到初始提交，包括当前提交
     */
    public static List<Commit> commitTraceBack(Commit currentCommit) {
        List<Commit> commitList = new LinkedList<>();
        Commit commitPtr = currentCommit;
        while (commitPtr != null) {
            commitList.add(commitPtr);
            commitPtr = readCommit(commitPtr.getParentId());
        }
        return commitList;
    }

    /**
     * 回溯到初始提交，包括当前提交
     * @note 区别在于此函数将返回提交ID
     */
    public static List<String> commitIdTraceBack(Commit currentCommit) {
        List<String> commitList = new LinkedList<>();
        Commit commitPtr = currentCommit;
        while (commitPtr != null) {
            commitList.add(getCommitId(commitPtr));
            commitPtr = readCommit(commitPtr.getParentId());
        }
        return commitList;
    }

    /**
     * 获取此提交的所有祖先，包括提交本身
     * @param visitedSet 应该是一个空集合
     * @return 提交ID列表（字符串）
     */
    public static List<String> commitAncestors(Commit commit, Set<String> visitedSet) {
        String parentId = commit.getParentId();
        String secondParentId = commit.getSecondParentId();
        visitedSet.add(getCommitId(commit));
        List<String> result = new LinkedList<>();
        result.add(getCommitId(commit));
        if (parentId != null && !visited(visitedSet, parentId)) {
            result.addAll(commitAncestors(readCommit(parentId), visitedSet));
        }
        if (secondParentId != null && !visited(visitedSet, secondParentId)) {
            result.addAll(commitAncestors(readCommit(secondParentId), visitedSet));
        }
        return result;
    }

    /**
     * 检查是否已访问
     */
    private static boolean visited(Set<String> visitedSet, String commitId) {
        return visitedSet.contains(commitId);
    }

    /**
     * 获取两个分支的分割点
     * @return 如果两个列表长度相同且有相同的提交列表，则返回null
     */
    public static Commit getSplitCommit(String branchName1, String branchName2) {
        String branch1CommitId = BranchUtils.getCommitId(branchName1);
        String branch2CommitId = BranchUtils.getCommitId(branchName2);
        Commit commit1 = readCommit(branch1CommitId);
        Commit commit2 = readCommit(branch2CommitId);
        List<Commit> branch1Traced = commitTraceBack(commit1);
        List<Commit> branch2Traced = commitTraceBack(commit2);
        Collections.reverse(branch1Traced); // bug：列表应该是 旧提交 --> 新提交！
        Collections.reverse(branch2Traced);
        int minLength = Math.min(branch1Traced.size(), branch2Traced.size());
        for (int i = 0; i < minLength; ++i) {
            // 第一个不同提交的前面提交是分割点
            if (!isSameCommit(branch1Traced.get(i), branch2Traced.get(i))) {
                return branch1Traced.get(i - 1);
            }
        }
        // 如果两个列表长度相同且有相同的提交列表，则返回null
        if (branch1Traced.size() == branch2Traced.size()) {
            return null;
        }
        // 在minLength范围内，两个列表有相同的提交，则返回较短列表的末尾元素
        return branch1Traced.size() < branch2Traced.size() ?
                branch1Traced.get(branch1Traced.size() - 1) : branch2Traced.get(branch1Traced.size() - 1);
    }


    /**
     * 使用图方法获取两个分支的分割点
     * @return 如果两个列表长度相同且有相同的提交列表，则返回null
     */
    public static Commit getSplitCommitWithGraph(String branchName1, String branchName2) {
        String branch1CommitId = BranchUtils.getCommitId(branchName1);
        String branch2CommitId = BranchUtils.getCommitId(branchName2);
        Commit commit1 = readCommit(branch1CommitId);
        Commit commit2 = readCommit(branch2CommitId);
        List<String> branch1AncestorsId = commitAncestors(commit1, new HashSet<>());
        List<String> branch2AncestorsId = commitAncestors(commit2, new HashSet<>());
        List<String> commonAncestors = new LinkedList<>();
        for (String commitId : branch1AncestorsId) {
            if (branch2AncestorsId.contains(commitId)) {
                commonAncestors.add(commitId);
            }
        }
        Map<String, Integer> inDegreeOfAncestors = inDegreeOfNodes(commonAncestors);
        for (String commitId : inDegreeOfAncestors.keySet()) {
            if (inDegreeOfAncestors.get(commitId) == 0) {
                return CommitUtils.readCommit(commitId);
            }
        }
        return null;
    }

    /**
     * 计算共同祖先中每个节点的入度
     * 可以通过这些提交指向的位置（出度）来计算
     * @return 映射：提交ID --> 入度
     */
    private static Map<String, Integer> inDegreeOfNodes(List<String> commitIds) {
        Map<String, Integer> statisticResult = new HashMap<>();
        for (String commitId : commitIds) {
            statisticResult.put(commitId, 0);
        }
        for (String commitId : commitIds) {
            Commit commit = CommitUtils.readCommit(commitId);
            String parentId = commit.getParentId();
            String secondParentId = commit.getSecondParentId();
            if (parentId != null) {
                statisticResult.put(parentId, statisticResult.get(parentId) + 1);
            }
            if (secondParentId != null) {
                statisticResult.put(secondParentId, statisticResult.get(secondParentId) + 1);
            }
        }
        return statisticResult;
    }

    /**
     * 返回两个提交是否有相同的文件版本，给定文件名
     * @return 如果其中一个提交不包含该文件，返回null，否则返回true或false
     */
    public static Boolean hasSameFileVersion(String fileName, Commit commit1, Commit commit2) {
        assert commit1 != null && commit2 != null && fileName != null;
        HashMap<String, String> fileVersionMap1 = commit1.getFileVersionMap();
        HashMap<String, String> fileVersionMap2 = commit2.getFileVersionMap();
        if (!fileVersionMap1.containsKey(fileName) || !fileVersionMap2.containsKey(fileName)) {
            return null;
        }
        return fileVersionMap1.get(fileName).equals(fileVersionMap2.get(fileName));
    }

    /**
     * 检查具有文件名的文件的一致性
     * 什么是一致性？它意味着两个提交：
     * 1. 都有该文件或都没有该文件，
     * 2. 如果都有该文件，必须有相同的文件版本
     */
    public static boolean isConsistent(String fileName, Commit commit1, Commit commit2) {
        assert commit1 != null && commit2 != null && fileName != null;
        HashMap<String, String> fileVersionMap1 = commit1.getFileVersionMap();
        HashMap<String, String> fileVersionMap2 = commit2.getFileVersionMap();
        boolean existInCommit1 = fileVersionMap1.containsKey(fileName);
        boolean existInCommit2 = fileVersionMap2.containsKey(fileName);
        if (!existInCommit1 && !existInCommit2) {
            return true;
        }
        if (!existInCommit1 || !existInCommit2) {
            return false;
        }
        Boolean sameContent = hasSameFileVersion(fileName, commit1, commit2);
        assert sameContent != null;
        return sameContent;
    }
}
