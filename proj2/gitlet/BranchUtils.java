package gitlet;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author Shiyu
 * @Description 分支工具类，提供分支相关的操作方法
 */
public class BranchUtils {
    /**
     * 获取远程分支文件夹对象，通过分支名称（例如：origin/master）
     */
    public static File getRemoteBranchFolder(String branchName) {
        assert branchName != null && branchName.contains("/");
        String[] split = branchName.split("/");
        return join(BRANCHES_DIR, split[0]);
    }

    /**
     * 获取远程分支文件对象，通过分支名称（例如：origin/master）
     */
    public static File getRemoteBranchFile(String branchName) {
        assert branchName != null && branchName.contains("/");
        String[] split = branchName.split("/");
        return join(BRANCHES_DIR, split[0], split[1]);
    }

    /**
     * 获取指定分支的提交ID
     */
    public static String getCommitId(String branchName) {
        if (branchName.contains("/")) { // 处理远程分支情况：origin/master
            File remoteBranchFile = getRemoteBranchFile(branchName);
            return readContentsAsString(remoteBranchFile);
        }
        return readContentsAsString(join(BRANCHES_DIR, branchName));
    }

    /**
     * 设置当前分支指向新的提交ID
     */
    public static void saveCommitId(String branchName, String commitId) {
        if (branchName.contains("/")) { // 处理远程分支情况：origin/master
            String[] split = branchName.split("/");
            File folder = join(BRANCHES_DIR, split[0]);
            if (!folder.exists()) {
                folder.mkdir();
            }
            Utils.writeContents(join(folder, split[1]), commitId);
            return;
        }
        Utils.writeContents(join(BRANCHES_DIR, branchName), commitId);
    }

    /**
     * 删除分支文件，不会检查分支是否存在，直接删除相关的分支文件
     * @note 此方法不会删除任何提交
     */
    public static boolean removeBranch(String branchName) {
        if (branchName.contains("/")) {
            return getRemoteBranchFile(branchName).delete();
        }
        return join(BRANCHES_DIR, branchName).delete();
    }

    /***
     * @return 按字典序排列的分支名称列表
     */
    public static List<String> getAllBranchNames() {
        List<String> branchNameList = plainFilenamesIn(BRANCHES_DIR);
        assert branchNameList != null;
        branchNameList = new LinkedList<>(branchNameList); // 分配新的内存空间以便修改此变量
        File[] remoteFolders = BRANCHES_DIR.listFiles(File::isDirectory);
        if (remoteFolders != null) {
            for (File remoteFolder : remoteFolders) {
                List<String> remoteBranches = plainFilenamesIn(remoteFolder);
                assert remoteBranches != null;
                for (String remoteName : remoteBranches) {
                    branchNameList.add(remoteFolder.getName() + "/" + remoteName);
                }
            }
        }
        branchNameList.sort(String::compareTo);
        return branchNameList;
    }

    /**
     * 查询分支是否存在于 .gitlet/branches 目录中
     * @param branchName 分支名称，不能为null，此函数会进行断言检查
     */
    public static boolean branchExists(String branchName) {
        if (branchName.contains("/")) {
            return getRemoteBranchFile(branchName).exists();
        }
        List<String> branchNameList = getAllBranchNames();
        return branchNameList.contains(branchName);
    }

}
