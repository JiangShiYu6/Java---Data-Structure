package gitlet;

import java.util.HashMap;
import java.util.List;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author Shiyu
 * @Description 文件工具类，提供文件相关的操作方法
 */
public class FileUtils {
    /***
     * 判断当前工作目录中的文件是否与目标SHA-1值相同
     * （这也意味着它们有相同的内容）
     */
    public static boolean hasSameSHA1(String fileName, String targetSHA1) {
        return getFileContentSHA1(fileName).equals(targetSHA1);
    }

    /**
     * 从.gitlet/objects目录读取某个版本文件的内容
     */
    public static String getFileContent(String fileSHA1) {
        return readContentsAsString(join(OBJECTS_DIR, fileSHA1));
    }

    /**
     * 从.gitlet/objects目录读取提交中某个版本文件的内容
     */
    public static String getFileContent(String fileName, Commit commit) {
        assert fileName != null && commit != null;
        return getFileContent(commit.getFileVersionMap().get(fileName));
    }

    /***
     * @param fileName 要作为对象保存在.gitlet/objects中的文件名
     * @return 文件内容的sha1值
     */
    public static String createGitletObjectFile(String fileName) {
        return writeGitletObjectsFile(readContentsAsString(join(CWD, fileName)));
    }

    /***
     * @param content 文件的字符串内容
     * @return 文件内容的sha1值
     */
    public static String writeGitletObjectsFile(String content) {
        String fileObjectId = sha1(content);
        writeContents(join(OBJECTS_DIR, fileObjectId), content);
        return fileObjectId;
    }

    /**
     * 将内容写入当前工作目录的文件
     */
    public static void writeCWDFile(String fileName, String content) {
        writeContents(join(CWD, fileName), content);
    }

    /**
     * 获取当前工作目录中文件内容的SHA1值
     */
    public static String getFileContentSHA1(String fileName) {
        return sha1(readContentsAsString(join(CWD, fileName)));
    }

    /**
     * 将某个提交跟踪的所有文件恢复到工作目录
     * 在"未跟踪文件"检查之后，不会删除之前提交跟踪的文件
     * 但是，当前工作目录中被之前提交跟踪但不被目标提交跟踪的文件将被删除
     * 一些文件将被创建，这些是目标提交跟踪但不在当前工作目录中的文件
     * @note 在调用此函数之前必须进行"未跟踪文件"检查
     */
    public static void restoreCommitFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        assert CWDFileNames != null;
        for (String CWDFileName : CWDFileNames) {
            // 删除此提交中未跟踪的文件
            if (!fileVersionMap.containsKey(CWDFileName)) {
                Utils.restrictedDelete(join(CWD, CWDFileName));
            }
        }
        // 将文件恢复到当前工作目录
        for (String fileName : fileVersionMap.keySet()) {
            writeCWDFile(fileName, getFileContent(fileVersionMap.get(fileName)));
        }
    }

    /**
     * 检查是否会覆盖或删除当前工作目录中的未跟踪文件
     * @param fileName 某个提交中将要恢复到当前工作目录或从当前工作目录删除的文件名
     */
    public static boolean isOverwritingOrDeletingCWDUntracked(String fileName, Commit currentCommit) {
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        assert CWDFileNames != null && currentCommit != null;
        return !CommitUtils.isTrackedByCommit(currentCommit, fileName) && CWDFileNames.contains(fileName);
    }
}
