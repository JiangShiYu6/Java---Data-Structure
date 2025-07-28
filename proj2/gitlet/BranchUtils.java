package gitlet;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * 分支工具类
 * 
 * 这个类提供了所有与Git分支相关的操作功能，包括：
 * 1. 本地分支管理：创建、删除、查询分支
 * 2. 远程分支管理：处理形如 origin/master 的远程分支
 * 3. 分支与提交的映射：每个分支都指向一个特定的提交ID
 * 
 * 设计思路：
 * - 本地分支：直接在 .gitlet/branches/ 目录下创建文件，文件名为分支名，内容为提交ID
 * - 远程分支：在 .gitlet/branches/ 下创建子目录（如origin），再在子目录中创建分支文件
 * 
 * @Author Shiyu
 * @Description 分支管理的核心工具类，处理所有分支相关操作
 */
public class BranchUtils {
    /**
     * 获取远程分支的文件夹对象
     * 
     * 功能说明：
     * 对于远程分支（如 origin/master），我们需要在 .gitlet/branches/ 下创建对应的远程仓库文件夹
     * 这个方法返回远程仓库的文件夹路径，比如 origin/master 会返回 .gitlet/branches/origin/
     * 
     * 使用场景：
     * - 创建远程分支时，需要先确保远程仓库文件夹存在
     * - 管理远程仓库的所有分支
     * 
     * @param branchName 远程分支名，格式必须为 "远程仓库名/分支名"，如 "origin/master"
     * @return 远程仓库文件夹的File对象，如 .gitlet/branches/origin/
     * @throws AssertionError 如果分支名为null或不包含"/"
     */
    public static File getRemoteBranchFolder(String branchName) {
        assert branchName != null && branchName.contains("/") 
            : "远程分支名必须包含'/'，格式如：origin/master";
        String[] split = branchName.split("/");
        return join(BRANCHES_DIR, split[0]);
    }

    /**
     * 获取远程分支的文件对象
     * 
     * 功能说明：
     * 返回远程分支对应的文件路径，这个文件存储了该分支指向的提交ID
     * 比如 origin/master 会返回 .gitlet/branches/origin/master 文件
     * 
     * 文件结构示例：
     * .gitlet/branches/
     * ├── master                    (本地master分支文件)
     * ├── develop                   (本地develop分支文件)  
     * └── origin/                   (远程仓库文件夹)
     *     ├── master                (远程master分支文件)
     *     └── develop               (远程develop分支文件)
     * 
     * @param branchName 远程分支名，格式必须为 "远程仓库名/分支名"，如 "origin/master"
     * @return 远程分支文件的File对象，如 .gitlet/branches/origin/master
     * @throws AssertionError 如果分支名为null或不包含"/"
     */
    public static File getRemoteBranchFile(String branchName) {
        assert branchName != null && branchName.contains("/") 
            : "远程分支名必须包含'/'，格式如：origin/master";
        String[] split = branchName.split("/");
        return join(BRANCHES_DIR, split[0], split[1]);
    }

    /**
     * 获取分支指向的提交ID
     * 
     * 功能说明：
     * 每个分支都指向一个特定的提交，这个方法读取分支文件中存储的提交ID
     * 支持本地分支和远程分支两种格式
     * 
     * 处理逻辑：
     * 1. 如果是远程分支（包含"/"）：读取 .gitlet/branches/远程仓库名/分支名 文件
     * 2. 如果是本地分支：读取 .gitlet/branches/分支名 文件
     * 
     * 使用场景：
     * - 切换分支时，需要知道要切换到哪个提交
     * - 合并分支时，需要知道两个分支分别指向哪些提交
     * - 显示分支状态时，需要展示分支的当前提交
     * 
     * @param branchName 分支名，可以是本地分支（如"master"）或远程分支（如"origin/master"）
     * @return 该分支指向的提交ID（SHA-1哈希值）
     */
    public static String getCommitId(String branchName) {
        if (branchName.contains("/")) { // 处理远程分支情况：origin/master
            File remoteBranchFile = getRemoteBranchFile(branchName);
            return readContentsAsString(remoteBranchFile);
        }
        // 处理本地分支情况：master, develop等
        return readContentsAsString(join(BRANCHES_DIR, branchName));
    }

    /**
     * 保存分支指向的提交ID
     * 
     * 功能说明：
     * 将指定的提交ID写入到分支文件中，使分支指向该提交
     * 这是Git中分支操作的核心：分支本质上就是指向提交的指针
     * 
     * 操作流程：
     * 1. 远程分支：先确保远程仓库文件夹存在，然后在其中创建/更新分支文件
     * 2. 本地分支：直接在branches目录下创建/更新分支文件
     * 
     * 使用场景：
     * - 创建新分支：将新分支指向当前提交或指定提交
     * - 提交代码后：更新当前分支指向新的提交
     * - 合并分支后：更新目标分支指向合并后的提交
     * - 重置分支：将分支指向历史中的某个提交
     * 
     * 文件内容示例：
     * 分支文件中只存储一行内容，就是40位的SHA-1提交ID
     * 如：a1b2c3d4e5f6789012345678901234567890abcd
     * 
     * @param branchName 分支名，可以是本地分支或远程分支
     * @param commitId 要指向的提交ID（40位SHA-1哈希值）
     */
    public static void saveCommitId(String branchName, String commitId) {
        if (branchName.contains("/")) { // 处理远程分支情况：origin/master
            String[] split = branchName.split("/");
            File folder = join(BRANCHES_DIR, split[0]);
            // 确保远程仓库文件夹存在
            if (!folder.exists()) {
                folder.mkdir();
            }
            // 在远程仓库文件夹中写入分支文件
            Utils.writeContents(join(folder, split[1]), commitId);
            return;
        }
        // 处理本地分支情况：直接在branches目录下写入
        Utils.writeContents(join(BRANCHES_DIR, branchName), commitId);
    }

    /**
     * 删除指定的分支
     * 
     * 功能说明：
     * 删除分支对应的文件，从而移除该分支
     * 注意：这个操作只删除分支指针，不会删除任何提交对象
     * 
     * 安全考虑：
     * - 不检查分支是否存在，直接尝试删除
     * - 不检查分支是否为当前分支，调用方需要自行检查
     * - 不删除提交历史，只删除分支引用
     * 
     * Git原理：
     * 在Git中，分支只是指向提交的指针，删除分支不会影响提交历史
     * 只要有其他分支或标签指向相同的提交，这些提交就不会丢失
     * 
     * 使用场景：
     * - 功能开发完成后，删除临时分支
     * - 清理不再需要的远程跟踪分支
     * - 重构代码时，删除过时的分支
     * 
     * @param branchName 要删除的分支名，可以是本地分支或远程分支
     * @return true如果成功删除文件，false如果文件不存在或删除失败
     * @note 此方法不会删除任何提交对象，只删除分支引用
     */
    public static boolean removeBranch(String branchName) {
        if (branchName.contains("/")) {
            // 删除远程分支文件
            return getRemoteBranchFile(branchName).delete();
        }
        // 删除本地分支文件
        return join(BRANCHES_DIR, branchName).delete();
    }

    /**
     * 获取所有分支名称列表
     * 
     * 功能说明：
     * 扫描整个分支目录，收集所有本地分支和远程分支的名称
     * 返回按字典序排序的完整分支列表
     * 
     * 扫描逻辑：
     * 1. 扫描 .gitlet/branches/ 目录下的所有文件，这些是本地分支
     * 2. 扫描 .gitlet/branches/ 目录下的所有子目录，这些是远程仓库
     * 3. 对每个远程仓库目录，扫描其中的分支文件
     * 4. 将远程分支名格式化为 "远程仓库名/分支名"
     * 5. 对所有分支名进行字典序排序
     * 
     * 返回示例：
     * [
     *   "develop",           // 本地分支
     *   "master",            // 本地分支  
     *   "origin/develop",    // 远程分支
     *   "origin/master",     // 远程分支
     *   "upstream/main"      // 另一个远程仓库的分支
     * ]
     * 
     * 使用场景：
     * - git branch 命令：显示所有分支
     * - git status 命令：显示分支状态
     * - 分支补全功能：为用户提供分支名提示
     * - 分支验证：检查用户输入的分支名是否存在
     * 
     * @return 按字典序排序的所有分支名称列表
     */
    public static List<String> getAllBranchNames() {
        // 获取所有本地分支（直接在branches目录下的文件）
        List<String> branchNameList = plainFilenamesIn(BRANCHES_DIR);
        assert branchNameList != null : "无法读取分支目录";
        
        // 创建新的列表以便修改（原列表可能是不可变的）
        branchNameList = new LinkedList<>(branchNameList);
        
        // 扫描远程仓库目录（branches目录下的子目录）
        File[] remoteFolders = BRANCHES_DIR.listFiles(File::isDirectory);
        if (remoteFolders != null) {
            for (File remoteFolder : remoteFolders) {
                // 获取该远程仓库的所有分支
                List<String> remoteBranches = plainFilenamesIn(remoteFolder);
                assert remoteBranches != null : "无法读取远程仓库目录: " + remoteFolder.getName();
                
                // 将远程分支添加到列表中，格式为 "远程仓库名/分支名"
                for (String remoteName : remoteBranches) {
                    branchNameList.add(remoteFolder.getName() + "/" + remoteName);
                }
            }
        }
        
        // 按字典序排序，确保输出的一致性
        branchNameList.sort(String::compareTo);
        return branchNameList;
    }

    /**
     * 检查指定分支是否存在
     * 
     * 功能说明：
     * 验证给定的分支名是否对应一个实际存在的分支
     * 支持检查本地分支和远程分支
     * 
     * 检查逻辑：
     * 1. 远程分支：直接检查对应的文件是否存在
     * 2. 本地分支：通过获取所有分支列表来检查
     * 
     * 性能考虑：
     * - 对于远程分支，直接文件检查效率更高
     * - 对于本地分支，使用getAllBranchNames()确保一致性
     * 
     * 使用场景：
     * - 分支切换前：验证目标分支是否存在
     * - 分支创建前：检查是否与现有分支重名
     * - 分支删除前：确认要删除的分支确实存在
     * - 合并操作前：验证源分支和目标分支都存在
     * 
     * 错误处理：
     * 如果传入null分支名，会触发断言错误
     * 
     * @param branchName 要检查的分支名，不能为null
     * @return true如果分支存在，false如果分支不存在
     * @throws AssertionError 如果branchName为null
     */
    public static boolean branchExists(String branchName) {
        assert branchName != null : "分支名不能为null";
        
        if (branchName.contains("/")) {
            // 远程分支：直接检查文件是否存在，效率更高
            return getRemoteBranchFile(branchName).exists();
        }
        
        // 本地分支：通过获取所有分支列表来检查，确保一致性
        List<String> branchNameList = getAllBranchNames();
        return branchNameList.contains(branchName);
    }

}
