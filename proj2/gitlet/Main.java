package gitlet;

import static gitlet.Help.checkIfInit;
import static gitlet.Repository.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if(args.length==0){
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validArgs(args,1);
                Repository.init();
                break;
            case "add":
                validArgs(args,2);
                checkIfInit();
                Repository.add(args[1]);
                break;
            case "commit":
                validArgs(args,2);
                checkIfInit();
                Repository.commit(args[1]);
                break;
            case "rm":
                validArgs(args,2);
                checkIfInit();
                Repository.rm(args[1]);
                break;
            case "log":
                validArgs(args,1);
                checkIfInit();
                Repository.log();
                break;
            case "global-log":
                validArgs(args,1);
                checkIfInit();
                Repository.global_log();
                break;
            case "find":
                validArgs(args,2);
                checkIfInit();
                Repository.find(args[1]);
                break;
            case "status":
                validArgs(args,1);
                checkIfInit();
                Repository.status();
                break;
            case "checkout":
                checkIfInit();
                if (args.length < 1 || args.length > 4) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                // === 模式 1: checkout -- [file name]
                if (args.length == 3 && args[1].equals("--")) {
                    String fileName = args[2];
                    Repository.checkoutFileFromHead(fileName);
                    return;
                }
                // === 模式 2: checkout [commit id] -- [file name]
                if (args.length == 4 && args[2].equals("--")) {
                    String commitID = args[1];
                    String fileName = args[3];
                    Repository.checkoutFileFromCommit(commitID, fileName);
                    return;
                }
                // === 模式 3: checkout [branch name]
                if (args.length == 2) {
                    String branchName = args[1];
                    Repository.checkoutBranch(branchName);
                    return;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            case "branch":
                validArgs(args,2);
                checkIfInit();
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                validArgs(args,2);
                checkIfInit();
                Repository.rm_branch(args[1]);
                break;
            case "reset":
                validArgs(args,2);
                checkIfInit();
                Repository.reset(args[1]);
                break;
            case "merge":
                validArgs(args,2);
                checkIfInit();
                Repository.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }

    }

    private static void validArgs(String[] args, int num) {
        if (args.length != num) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
