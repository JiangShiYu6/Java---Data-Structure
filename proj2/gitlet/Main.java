package gitlet;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static gitlet.GitletConstants.*;

/**
 * @description Gitlet的驱动类，Git版本控制系统的一个子集。
 * 提示：checkout命令也会恢复索引区域（所谓的"工作树清理"）
 * checkout只是将HEAD指针移动到某个提交（可能在另一个分支中）
 *
 * 使用HEAD文件存储例如 HEAD --> master 信息
 * 使用branches目录存储不同的分支及其指向的提交
 * 例如：master分支使用名为[master]的文件并在其中存储[提交ID(SHA-1)]
 *
 * 提示：runnable没有参数也没有返回值！你可以将runnable视为普通类
 */
public class Main {

    /**
     * @note
     * 其他需要提供的错误
     * 1. 输入命令的操作数数量或格式错误 --> 操作数错误
     * 2. 命令必须在.gitlet文件夹已创建但尚未创建的情况下 --> 不在初始化的Gitlet目录中
     * */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        String[] restArgs = Arrays.copyOfRange(args, 1, args.length);
        switch(firstArg) {
            case "init":
                if (restArgs.length == 0) {
                    Repository.init();
                } else {
                    System.out.println(INCORRECT_OPERANDS_WARNING);
                }
                break;
            case "add":
                commandRunner(restArgs.length == 1, Repository::add, restArgs[0]);
                break;
            case "commit":
                commandRunner(restArgs.length == 1, Repository::commit, restArgs[0]);
                break;
            case "rm":
                commandRunner(restArgs.length == 1, Repository::rm, restArgs[0]);
                break;
            case "log":
                commandRunner(restArgs.length == 0, Repository::log);
                break;
            case "global-log":
                commandRunner(restArgs.length == 0, Repository::globalLog);
                break;
            case "checkout":
                commandRunner(restArgs.length >= 1 && restArgs.length <= 3, Repository::checkout, restArgs);
                break;
            case "branch":
                commandRunner(restArgs.length == 1, Repository::branch, restArgs[0]);
                break;
            case "find":
                commandRunner(restArgs.length == 1, Repository::find, restArgs[0]);
                break;
            case "status":
                commandRunner(restArgs.length == 0, Repository::status);
                break;
            case "rm-branch":
                commandRunner(restArgs.length == 1, Repository::removeBranch, restArgs[0]);
                break;
            case "reset":
                commandRunner(restArgs.length == 1, Repository::reset, restArgs[0]);
                break;
            case "merge":
                commandRunner(restArgs.length == 1, Repository::merge, restArgs[0]);
                break;
            case "add-remote":
                commandRunner(restArgs.length == 2, RemoteUtils::addRemote, restArgs[0], restArgs[1]);
                break;
            case "rm-remote":
                commandRunner(restArgs.length == 1, RemoteUtils::removeRemote, restArgs[0]);
                break;
            case "push":
                commandRunner(restArgs.length == 2, RemoteUtils::push, restArgs[0], restArgs[1]);
                break;
            case "fetch":
                commandRunner(restArgs.length == 2, RemoteUtils::fetch, restArgs[0], restArgs[1]);
                break;
            case "pull":
                commandRunner(restArgs.length == 2, RemoteUtils::pull, restArgs[0], restArgs[1]);
                break;
            case "test":
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }

    /***
     * 同时检查一个命令是否在init()之后以及检查参数数量
     * @param argsNumberCheck 我们建议你添加逻辑表达式，如：restArgs.length != 0
     * @param function 作为lambda的Function接口
     */
    private static <T> void commandRunner(boolean argsNumberCheck, Consumer<T> function, T args) {
        if (!Repository.isInitialized()) {
            System.out.println(UNINITIALIZED_WARNING);
            return;
        }
        if (!argsNumberCheck) {
            System.out.println(INCORRECT_OPERANDS_WARNING);
            return;
        }
        function.accept(args);
    }

    /***
     * 同时检查一个命令是否在init()之后以及检查参数数量
     * @param argsNumberCheck 我们建议你添加逻辑表达式，如：restArgs.length != 0
     * @param function 作为lambda的Function接口
     */
    private static <T1, T2> void commandRunner(boolean argsNumberCheck, BiConsumer<T1, T2> function, T1 args1, T2 args2) {
        if (!Repository.isInitialized()) {
            System.out.println(UNINITIALIZED_WARNING);
            return;
        }
        if (!argsNumberCheck) {
            System.out.println(INCORRECT_OPERANDS_WARNING);
            return;
        }
        function.accept(args1, args2);
    }

    /***
     * 与上面的函数类似，但没有参数
     * @param argsNumberCheck 我们建议你添加逻辑表达式，如：restArgs.length != 0
     * @param function 作为lambda的Function接口
     */
    private static void commandRunner(boolean argsNumberCheck, Runnable function) {
        if (!Repository.isInitialized()) {
            System.out.println(UNINITIALIZED_WARNING);
            return;
        }
        if (!argsNumberCheck) {
            System.out.println(INCORRECT_OPERANDS_WARNING);
            return;
        }
        function.run();
    }
}
