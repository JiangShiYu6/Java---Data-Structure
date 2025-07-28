package gitlet;

import java.io.File;

import static gitlet.Utils.join;

/**
 * @Author Shiyu
 * @Description Gitlet常量定义类
 */
public class GitletConstants {
    public static final String MASTER_BRANCH_NAME = "master";
    /** 当前工作目录 */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** .gitlet目录 */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** 索引文件 */
    public static final File INDEX_FILE = join(GITLET_DIR, "index");
    /** 存储映射，远程名称 --> 位置(路径) */
    public static final File REMOTE_FILE = join(GITLET_DIR, "remote");
    /** 每个远程仓库必须有自己的HEAD和master分支 */
    public static final File REMOTE_FILE_DIR = join(GITLET_DIR, "remotes");
    /** HEAD文件 */
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");
    /** 提交目录，存储每个提交 */
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    /** 对象目录，存储具体的文件 */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** 分支目录 */
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    /** 暂存文件 */
    public static final File STAGED_FILE = join(GITLET_DIR, "staged-files");

    /** 未初始化警告信息 */
    public static final String UNINITIALIZED_WARNING = "Not in an initialized Gitlet directory.";
    /** 操作数不正确警告信息 */
    public static final String INCORRECT_OPERANDS_WARNING = "Incorrect operands.";

    /** 合并时修改未跟踪文件的警告信息 */
    public static final String MERGE_MODIFY_UNTRACKED_WARNING = 
        "There is an untracked file in the way; delete it, or add and commit it first.";
}
