/** Class that prints the Collatz sequence starting from a given number.
 *  @author YOUR NAME HERE
 */
public class Collatz {

    /** Correct implementation of nextNumber based on Collatz conjecture. */
    public static int nextNumber(int n) {
        if (n % 2 == 0) {  // 如果 n 是偶数
            return n / 2;
        } else {  // 如果 n 是奇数
            return 3 * n + 1;
        }
    }

    public static void main(String[] args) {
        int n = 5;  // 可以更改初始值来测试其他数字
        System.out.print(n + " ");
        while (n != 1) {
            n = nextNumber(n);
            System.out.print(n + " ");
        }
        System.out.println();  // 换行
    }
}