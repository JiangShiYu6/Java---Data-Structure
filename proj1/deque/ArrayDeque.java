package deque;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayDeque<T> implements Deque<T>, Iterable<T> { // 实现 Iterable<T>
    private T[] items;
    private int size;
    private int front;
    private int back;
    private static final int INITIAL_CAPACITY = 8;

    public ArrayDeque() {
        items = (T[]) new Object[INITIAL_CAPACITY];
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
        items[front] = null;
        front = (front + 1) % items.length;
        size--;
        checkResize();
        return removedItem;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) return null;
        back = (back - 1 + items.length) % items.length;
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
        back = size;
    }

    private void checkResize() {
        if (items.length >= 16 && size < items.length / 4) {
            resize(items.length / 2);
        }
    }

    // **实现迭代器**
    @Override
    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int index = 0; // 记录遍历到的元素个数
        private int current = front; // 当前遍历到的索引

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T item = items[current];
            current = (current + 1) % items.length; // 循环数组移动
            index++;
            return item;
        }
    }
}
