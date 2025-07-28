package gitlet;



import static gitlet.GitletConstants.HEAD_FILE;
import static gitlet.GitletConstants.GITLET_DIR;
import static gitlet.Utils.writeContents;

public class Help {
        /**
     * 设置HEAD --> 分支名（其他函数可能涉及设置head到提交，
     * 但这个项目将忽略这种情况）
     * 同时，它保存HEAD文件
     * @param branchName 参数必须存在，否则会抛出AssertionError
     * */
    public static void setHEAD(String branchName) {
        assert BranchUtils.branchExists(branchName);
        Repository.HEAD = branchName;
        writeContents(HEAD_FILE, branchName);
    }

    /***
     * head --> 分支名 --> 提交id
     */
    public static String getHeadCommitId() {
        return BranchUtils.getCommitId(Repository.HEAD);
    }

    /**
     * @return boolean: 检查此项目是否已初始化gitlet
     * */
    public static boolean isInitialized() {
        return GITLET_DIR.exists();
    }
}
