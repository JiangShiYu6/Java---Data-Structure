package timingtest;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeSLList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void timeGetLast() {
        AList<Integer> Ns = new AList<>();
        AList<Double> times = new AList<>();
        AList<Integer> opCounts = new AList<>();

        int[] testSizes = {1000, 2000, 4000, 8000, 16000, 32000, 64000, 128000};
        int M = 10000;  // 进行 10,000 次 getLast 操作

        for (int N : testSizes) {
            SLList<Integer> testList = new SLList<>();

            // **填充 SLList**（这一步是必须的！）
            for (int i = 0; i < N; i++) {
                testList.addLast(i);
            }

            // **启动计时器**
            Stopwatch sw = new Stopwatch();

            // **执行 M 次 getLast 操作**
            for (int i = 0; i < M; i++) {
                testList.getLast();
            }

            // **记录时间**
            double timeInSeconds = sw.elapsedTime();

            // **存储数据**
            Ns.addLast(N);
            times.addLast(timeInSeconds);
            opCounts.addLast(M);
        }

        // **打印测试结果**
        printTimingTable(Ns, times, opCounts);
    }

    public static void main(String[] args) {
        timeGetLast();
    }
}
