package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author S
 * @Date 2024/2/24 16:07
 * @Description 此类正确用法是：在内存中更改任何内容，最后保存更改。
 * 是的，每个更改索引的命令都必须调用saveIndex()方法来永久保存其更改
 */
public class IndexUtils {
    /** 读取INDEX文件（存储映射，文件名 --> 版本），它表示下一个提交的名称 --> 版本映射，
     * 这意味着在一个提交之后，indexMap等于提交的fileVersionMap */
    public static HashMap<String, String> indexMap;
    /** 暂存文件，暂存文件ID（sha1） --> 文件内容，暂存或取消暂存文件 */
    public static HashMap<String, String> stagedFileContents;

    static {
        if (Repository.isInitialized()) {
            indexMap = readIndex();
            stagedFileContents = readStagedContents();
        }
    }

    /***
     * 此函数将indexMap和stagedFileContents写入INDEX_FILE和STAGED_FILE
     * 对索引的每次更改都必须保存
     * @note 必须将stagedFileContents存储到STAGED_FILE并在一次提交后清除它！
     */
    public static void saveIndex() {
        Utils.writeObject(INDEX_FILE, indexMap);
        Utils.writeObject(STAGED_FILE, stagedFileContents);
    }

    /**
     * 在indexMap和stagedFileContents中暂存文件（注意：如果文件错误，将抛出异常）
     * @note 此函数不会保存任何内容到磁盘，只是在内存中保持它们
     */
    public static void stageFile(String fileName) {
        // 更新：为了节省空间，我们可以在索引映射中只保存sha1，而不是真正创建文件对象，
        // 而是将文件存储在映射（暂存）中并保存它。在提交时，我们创建对象
        String fileContents = readContentsAsString(join(CWD, fileName));
        String fileSHA1 = sha1(fileContents);
        indexMap.put(fileName, fileSHA1);
        stagedFileContents.put(fileSHA1, fileContents); // 在内存中保存文件内容，而不是磁盘
    }

    /***
     * 在内存中取消暂存文件
     * @note 此函数不会保存任何内容到磁盘，只是在内存中保持它们
     * @note stagedFileContents中可能有冗余条目，但一旦提交就会最终被清除
     */
    public static void unstageFile(String fileName) {
        String fileSHA1 = sha1(indexMap.get(fileName));
        stagedFileContents.remove(fileSHA1);
        indexMap.remove(fileName);
    }

    /**
     * 读取索引文件
     */
    public static HashMap<String, String> readIndex() {
        return hashMapRead(INDEX_FILE);
    }

    /**
     * 读取暂存内容
     */
    public static HashMap<String, String> readStagedContents() {
        return hashMapRead(STAGED_FILE);
    }

    /***
     * readIndex和readStagedContents的辅助函数
     */
    public static HashMap<String, String> hashMapRead(File file) {
        if (file.length() == 0) {
            return new HashMap<>();
        }
        // bug：必须检查索引文件是否为空以避免EOF异常
        HashMap<String, String> hashMap = Utils.readObject(file, HashMap.class);
        return hashMap != null ? hashMap : new HashMap<>();
    }

    /***
     * 获取git status的暂存文件
     * 它比较indexMap和commit.fileVersionMap
     * @param commit 要比较的当前提交
     * @return 文件名列表
     */
    public static List<String> getStagedFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> result = new LinkedList<>();
        for (String fileName : indexMap.keySet()) {
            if (fileVersionMap.containsKey(fileName)) {
                if (!fileVersionMap.get(fileName).equals(indexMap.get(fileName))) {
                    result.add(fileName);
                }
            } else {
                result.add(fileName);
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    /***
     * 所谓的已删除文件，是从indexMap中取消暂存的
     * 这也意味着文件在commit.getVersionMap()中但不在indexMap中
     * @param commit 要比较的当前提交
     */
    public static List<String> getRemovedFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> result = new LinkedList<>();
        for (String fileName : fileVersionMap.keySet()) {
            if (!indexMap.containsKey(fileName)) {
                result.add(fileName);
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    /**
     * 暂存文件是：在indexMap中但不在提交fileVersionMap中的文件；
     * 或在indexMap和提交fileVersionMap中但版本不同的文件。
     * indexMap中这些暂存的（文件 --> 版本）最终将在.gitlet/objects中创建
     */
    public static boolean isStaged(String fileName, Commit commit) {
        assert fileName != null && commit != null;
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        return (indexMap.containsKey(fileName) && !fileVersionMap.containsKey(fileName))
                || (indexMap.containsKey(fileName) && fileVersionMap.containsKey(fileName)
                    && !fileVersionMap.get(fileName).equals(indexMap.get(fileName)));
    }

    /**
     * 这些删除文件最终将在下一个提交fileVersionMap中被丢弃
     */
    public static boolean isRemoval(String fileName, Commit commit) {
        assert fileName != null && commit != null;
        return commit.getFileVersionMap().containsKey(fileName) && !indexMap.containsKey(fileName);
    }

    /**
     * （"未跟踪文件"）是指存在于工作目录中但既未暂存添加也未跟踪的文件
     * @note 也许这个方法应该在CommitUtils中实现
     */
    public static List<String> getUntrackedFiles(Commit commit) {
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        List<String> result = new LinkedList<>();
        assert CWDFileNames != null;
        for (String fileName : CWDFileNames) {
            if (!isStaged(fileName, commit) && !CommitUtils.isTrackedByCommit(commit, fileName)) {
                result.add(fileName);
            }
        }
        return result;
    }

    /**
     * "已修改但未暂存"
     * 已暂存添加，但与工作目录中的内容不同；（已修改）或
     * 在当前提交中跟踪，在工作目录中更改，但未暂存；（已删除）或
     * 已暂存添加，但在工作目录中删除；或
     * 未暂存删除，但在当前提交中跟踪并从工作目录中删除
     * @return 已修改但未暂存的文件名列表
     */
    public static List<StringBuffer> modifiedNotStagedForCommit(Commit commit) {
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        List<StringBuffer> result = new LinkedList<>();
        assert CWDFileNames != null;
        for (String fileName : CWDFileNames) {
            boolean fileIsStaged = isStaged(fileName, commit);
            boolean fileIsTracked = CommitUtils.isTrackedByCommit(commit, fileName);
            if ((fileIsStaged && !FileUtils.hasSameSHA1(fileName, indexMap.get(fileName))) ||
                    (fileIsTracked && !FileUtils.hasSameSHA1(fileName, commit.getFileVersionMap().get(fileName)) && !fileIsStaged)) {
                result.add(new StringBuffer(fileName));
            }
        }
        return result;
    }

    /**
     * 已暂存添加，但在工作目录中删除；或
     * 未暂存删除，但在当前提交中跟踪并从工作目录中删除
     * @return 已删除但未暂存的文件名列表
     */
    public static List<StringBuffer> deletedNotStagedForCommit(Commit commit) {
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        assert CWDFileNames != null;
        List<StringBuffer> result = new LinkedList<>();
        List<String> stagedFiles = getStagedFiles(commit);
        for (String fileName : stagedFiles) {
            if (!CWDFileNames.contains(fileName)) {
                result.add(new StringBuffer(fileName));
            }
        }
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        for (String fileName : fileVersionMap.keySet()) {
            if (!CWDFileNames.contains(fileName) && !isRemoval(fileName, commit)) {
                result.add(new StringBuffer(fileName));
            }
        }
        return result;
    }
}
