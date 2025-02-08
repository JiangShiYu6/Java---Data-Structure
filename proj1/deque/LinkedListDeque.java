package deque;

public class LinkedListDeque<T> implements Deque<T> {
    private class Node {
        T item;
        Node prev;
        Node next;

        Node(T item, Node prev, Node next) {
            this.item = item;
            this.prev = prev;
            this.next = next;
        }
    }

    private Node sentinel;
    private int size;

    /** 创建一个空的双向循环链表 */
    public LinkedListDeque() {
        sentinel = new Node(null, null, null);
        sentinel.next = sentinel;
        sentinel.prev = sentinel;
        size = 0;
    }

    /** 在头部插入一个元素，O(1) */
    @Override
    public void addFirst(T item) {
        Node newNode = new Node(item, sentinel, sentinel.next);
        sentinel.next.prev = newNode;
        sentinel.next = newNode;
        size++;
    }

    /** 在尾部插入一个元素，O(1) */
    @Override
    public void addLast(T item) {
        Node newNode = new Node(item, sentinel.prev, sentinel);
        sentinel.prev.next = newNode;
        sentinel.prev = newNode;
        size++;
    }

    /** 返回 deque 大小，O(1) */
    @Override
    public int size() {
        return size;
    }

    /** 打印 deque 所有元素 */
    @Override
    public void printDeque() {
        Node pos = sentinel.next;
        while (pos != sentinel) {
            System.out.print(pos.item + " ");
            pos = pos.next;
        }
        System.out.println();
    }

    /** 删除并返回头部元素，O(1) */
    @Override
    public T removeFirst() {
        if (sentinel.next == sentinel) return null;

        Node first = sentinel.next;
        sentinel.next = first.next;
        first.next.prev = sentinel;

        size--;

        // 避免内存泄漏
        first.next = null;
        first.prev = null;

        return first.item;
    }

    /** 删除并返回尾部元素，O(1) */
    @Override
    public T removeLast() {
        if (sentinel.prev == sentinel) return null;

        Node last = sentinel.prev;
        sentinel.prev = last.prev;
        last.prev.next = sentinel;

        size--;

        // 避免内存泄漏
        last.next = null;
        last.prev = null;

        return last.item;
    }

    /** 通过索引获取元素，使用迭代，O(n) */
    @Override
    public T get(int index) {
        if (index >= size || index < 0) {
            return null; // 规范要求返回 null，而不是抛异常
        }
        Node getNode = sentinel.next;
        for (int i = 0; i < index; i++) {
            getNode = getNode.next;
        }
        return getNode.item;
    }
}
