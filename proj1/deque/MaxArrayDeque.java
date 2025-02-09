package deque;

import java.util.Comparator;

public class MaxArrayDeque<T> extends ArrayDeque<T> {
    private Comparator<T> comparator;

    public MaxArrayDeque(Comparator<T> c) {
        super(); // 调用 ArrayDeque 的构造方法
        this.comparator = c;
    }

    public T max() {
        return max(comparator); // 复用 max(Comparator<T> c)
    }

    public T max(Comparator<T> c) {
        if (isEmpty()){ return null;}

        T maxElement = get(0); // 假设第一个元素是最大值
        for (int i = 1; i < size(); i++) {
            if (c.compare(get(i), maxElement) > 0) {
                maxElement = get(i); // 更新最大值
            }
        }
        return maxElement;
    }
}
