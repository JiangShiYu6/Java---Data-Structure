package gitlet;



import static gitlet.GitletConstants.HEAD_FILE;
import static gitlet.GitletConstants.GITLET_DIR;
import static gitlet.Utils.writeContents;

public class Help {
        /**
     * It set HEAD --> branch_name (other function maybe about set head on commit,
     * but this project will ignore this situation)
     * At the same time, it saves the HEAD file
     * @param branchName the param must exist, otherwise it will throw AssertionError
     * */
    public static void setHEAD(String branchName) {
        assert BranchUtils.branchExists(branchName);
        Repository.HEAD = branchName;
        writeContents(HEAD_FILE, branchName);
    }

    /***
     * head --> branch name --> commit id
     */
    public static String getHeadCommitId() {
        return BranchUtils.getCommitId(Repository.HEAD);
    }

    /**
     * @return boolean: checkout if this project is gitlet initialized
     * */
    public static boolean isInitialized() {
        return GITLET_DIR.exists();
    }
}
