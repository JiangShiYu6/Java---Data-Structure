package bstmap;

import java.util.*;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {
    private class BSTNode{
        K key;
        V value;
        BSTNode left, right;
        BSTNode(K key,V value){
            this.key=key;
            this.value=value;
            this.left = null;
            this.right = null;
        }
    }
    private BSTNode root;
    private int size = 0;

    @Override
    public void clear() {
        // TODO: 实现清除所有元素的逻辑
        root = null; // 断开所有连接，Java 垃圾回收会自动处理节点
        size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        // TODO: 实现键是否存在的判断
        return containsKey(root,key);
    }
    private boolean containsKey(BSTNode node,K key){
        if (node == null) {
            return false;
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            return containsKey(node.left, key);
        } else if (cmp > 0) {
            return containsKey(node.right, key);
        } else {
            return true; // 找到了
        }
    }

    @Override
    public V get(K key) {
        return get(root, key); // 从根节点开始查找
    }
    private V get(BSTNode node, K key) {
        if (node == null) {
            return null; // 没找到
        }
        int cmp = key.compareTo(node.key); // 比较 key 的大小
        if(cmp>0){
            return get(node.right, key);
        } else if (cmp<0) {
            return get(node.left,key);
        }
        else{
            return node.value;
        }
    }

    @Override
    public int size() {
        // TODO: 实现返回大小
        return size;
    }

    @Override
    public void put(K key, V value) {
        // TODO: 实现插入逻辑
        root = put(root, key, value);
    }
    private BSTNode put(BSTNode node, K key, V value){
        if(node==null){
            size++;
            return new BSTNode(key,value);
        }
        int cmp= key.compareTo(node.key);
        if (cmp < 0) {
            node.left = put(node.left, key, value);   // key 小 → 左边
        } else if (cmp > 0) {
            node.right = put(node.right, key, value); // key 大 → 右边
        }
        return node;
    }
    public void printInOrder() {
        printInOrder(root);
    }

    private void printInOrder(BSTNode node) {
        if (node == null) {
            return;
        }
        printInOrder(node.left); // 左子树
        System.out.println(node.key + " => " + node.value); // 当前节点
        printInOrder(node.right); // 右子树
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        collectKeys(root, keys);
        return keys;
    }

    private void collectKeys(BSTNode node, Set<K> keys) {
        if (node == null) {
            return;
        }
        keys.add(node.key);
        collectKeys(node.left, keys);
        collectKeys(node.right, keys);
    }

    @Override
    public V remove(K key) {
        V value = get(key);
        if (value != null) {
            root = remove(root, key);
            size--;
        }
        return value;
    }

    private BSTNode remove(BSTNode node, K key) {
        if (node == null) return null;

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = remove(node.left, key);
        } else if (cmp > 0) {
            node.right = remove(node.right, key);
        } else {
            // 找到了，删除它
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;

            // 两个子节点：用右子树最小节点替代
            BSTNode min = getMin(node.right);
            min.right = deleteMin(node.right);
            min.left = node.left;
            node = min;
        }
        return node;
    }

    private BSTNode getMin(BSTNode node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private BSTNode deleteMin(BSTNode node) {
        if (node.left == null) return node.right;
        node.left = deleteMin(node.left);
        return node;
    }
    @Override
    public V remove(K key, V value) {
        V currentVal = get(key);
        if (currentVal != null && currentVal.equals(value)) {
            return remove(key); // 复用已有逻辑
        }
        return null;
    }

    @Override
    public Iterator<K> iterator() {
        List<K> keys = new ArrayList<>();
        inOrderKeys(root, keys);
        return keys.iterator();
    }

    private void inOrderKeys(BSTNode node, List<K> keys) {
        if (node == null) return;
        inOrderKeys(node.left, keys);
        keys.add(node.key);
        inOrderKeys(node.right, keys);
    }
}