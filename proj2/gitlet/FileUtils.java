package gitlet;

import java.util.HashMap;
import java.util.List;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author Shiyu
 * @Description 文件工具类
 */
public class FileUtils {
    /***
     * 判断CWD中的文件是否与targetSHA1有相同的sha-1
     * （这也意味着它们有相同的内容）
     */
    public static boolean hasSameSHA1(String fileName, String targetSHA1) {
        return getFileContentSHA1(fileName).equals(targetSHA1);
    }

    /**
     * 从.gitlet/objects读取某个版本文件的内容
     */
    public static String getFileContent(String fileSHA1) {
        return readContentsAsString(join(OBJECTS_DIR, fileSHA1));
    }

    /**
     * 从.gitlet/objects读取提交版本中文件的内容
     */
    public static String getFileContent(String fileName, Commit commit) {
        assert fileName != null && commit != null;
        return getFileContent(commit.getFileVersionMap().get(fileName));
    }

    /***
     * @param fileName 要保存为.gitlet/objects中对象的文件名
     * @return 文件内容的sha1
     */
    public static String createGitletObjectFile(String fileName) {
        return writeGitletObjectsFile(readContentsAsString(join(CWD, fileName)));
    }

    /***
     * @param content 文件的字符串内容
     * @return 文件内容的sha1
     */
    public static String writeGitletObjectsFile(String content) {
        String fileObjectId = sha1(content);
        writeContents(join(OBJECTS_DIR, fileObjectId), content);
        return fileObjectId;
    }

    public static void writeCWDFile(String fileName, String content) {
        writeContents(join(CWD, fileName), content);
    }

    public static String getFileContentSHA1(String fileName) {
        return sha1(readContentsAsString(join(CWD, fileName)));
    }

    /**
     * restore all files tracked of one commit to work directory.
     * after the "untracked file" check, no file tracked by pre-commit will be deleted.
     * however, some files in CWD tracked by pre-commit and not tracked by 
     * after-commit will be deleted.
     * some files will be created, which is after-commit tracked files, but not in CWD.
     * @note you must do "untracked file" check before calling this function
     */
    public static void restoreCommitFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> cwdFileNames = plainFilenamesIn(CWD);
        assert cwdFileNames != null;
        for (String cwdFileName : cwdFileNames) {
            // 删除此提交中未跟踪的文件
            if (!fileVersionMap.containsKey(cwdFileName)) {
                Utils.restrictedDelete(join(CWD, cwdFileName));
            }
        }
        // 将文件恢复到CWD
        for (String fileName : fileVersionMap.keySet()) {
            writeCWDFile(fileName, getFileContent(fileVersionMap.get(fileName)));
        }
    }

    /**
     * @param fileName the file name of some commit which will be restored to CWD 
     *                 or deleted in CWD
     */
    public static boolean isOverwritingOrDeletingCWDUntracked(String fileName, 
                                                            Commit currentCommit) {
        List<String> cwdFileNames = plainFilenamesIn(CWD);
        assert cwdFileNames != null && currentCommit != null;
        return !CommitUtils.isTrackedByCommit(currentCommit, fileName) 
            && cwdFileNames.contains(fileName);
    }
}
