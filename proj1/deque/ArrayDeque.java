package deque;

public class ArrayDeque<T> implements Deque<T> {
    private T[] items;  // 存储数据的数组
    private int size;    // 当前 Deque 的元素个数
    private int front;   // 头部索引
    private int back;    // 尾部索引
    private static final int INITIAL_CAPACITY = 8; // 数组初始大小

    public ArrayDeque() {
        items = (T[]) new Object[INITIAL_CAPACITY]; // 创建泛型数组
        size = 0;
        front = 0;
        back = 0;
    }

    @Override
    public void addFirst(T item) {
        if (items.length == size) {
            resize(items.length * 2);
        }
        front = (front - 1 + items.length) % items.length;
        items[front] = item;
        size++;
    }

    @Override
    public void addLast(T item) {
        if (items.length == size) {
            resize(items.length * 2);
        }
        items[back] = item;
        back = (back + 1) % items.length;
        size++;
    }

    @Override
    public void printDeque() {
        for (int i = 0; i < size; i++) {
            System.out.print(items[(front + i) % items.length] + " ");
        }
        System.out.println();
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) return null;
        T removedItem = items[front];
        items[front] = null; // 避免内存泄漏
        front = (front + 1) % items.length; // 更新 front
        size--;
        checkResize(); // 检查是否需要缩小数组
        return removedItem;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) return null;
        back = (back - 1 + items.length) % items.length; // 更新 back
        T removedItem = items[back];
        items[back] = null;
        size--;
        checkResize();
        return removedItem;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) return null;
        return items[(front + index) % items.length];
    }

    @Override
    public int size() {
        return size;
    }

    private void resize(int newCapacity) {
        T[] newItems = (T[]) new Object[newCapacity];

        for (int i = 0; i < size; i++) {
            newItems[i] = items[(front + i) % items.length];
        }

        items = newItems;
        front = 0;
        back = size; // back 直接等于 size，指向下一个可用索引
    }

    private void checkResize() {
        if (items.length >= 16 && size < items.length / 4) {
            resize(items.length / 2);
        }
    }
}
