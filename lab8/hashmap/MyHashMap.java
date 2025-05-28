package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author Shiyu
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }
    private int size;
    private double maxLoadFactor;
    private static final int initialSize = 16;
    private static final double DEFAULT_MAX_LOAD = 0.75;
    /* Instance Variables */
    private Collection<Node>[] buckets;

    /** Constructors */
    public MyHashMap(int initialSize) {
        this(initialSize, DEFAULT_MAX_LOAD); // 调用另一个构造函数
    }
    public MyHashMap() {
        this(initialSize, DEFAULT_MAX_LOAD);
    }


    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    @SuppressWarnings("unchecked")
    public MyHashMap(int initialSize, double maxLoad) {
        this.maxLoadFactor = maxLoad;
        this.size = 0;
        buckets = createTable(initialSize);  // 初始化桶数组
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();  // 返回一个 LinkedList 类型的桶
    }
    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        Collection<Node>[] table = (Collection<Node>[]) new Collection[tableSize];
        for (int i = 0; i < tableSize; i++) {
            table[i] = createBucket(); // 每个桶是一个 LinkedList
        }
        return table;
    }
    @Override
    public void clear() {
        for (int i = 0; i < buckets.length; i++) {
            buckets[i].clear();  // 清空每个桶
        }
        size = 0;  // 维护 size 为 0
    }
    @Override
    public void put(K key, V value){
        int index=Math.floorMod(key.hashCode(), buckets.length);
        Collection<Node> bucket = buckets[index];
        // 遍历桶中的节点，查找是否已有相同的 key
        for(Node node:bucket){
            if(node.key.equals(key)){
                node.value=value;
                return;
            }
        }
        // 如果没有找到相同的 key，添加新的节点
        bucket.add(createNode(key, value));
        size++;
        // 检查是否需要扩容
        if ((double) size / buckets.length > maxLoadFactor) {
            resize(buckets.length * 2);
        }

    }
    private void resize(int newSize){
        Collection<Node>[] newBuckets = createTable(newSize);  // 创建新的桶数组
        // 将旧的节点重新哈希到新的桶数组
        for(Collection<Node>bucket:buckets){
            for(Node node:bucket){
                int newIndex = Math.floorMod(node.key.hashCode(), newSize);  // 重新计算桶索引
                newBuckets[newIndex].add(node);
            }
        }
        buckets = newBuckets;  // 更新桶数组
    }
    @Override
    public V get(K key){
        int index=Math.floorMod(key.hashCode(), buckets.length);
        Collection<Node> bucket = buckets[index];
        for(Node node:bucket){
            if(node.key.equals(key)){
                return node.value;
            }
        }
        return null;
    }
    @Override
    public boolean containsKey(K key){
        int index=Math.floorMod(key.hashCode(), buckets.length);
        Collection<Node> bucket = buckets[index];
        for(Node node:bucket){
            if(node.key.equals(key)){
                return true;
            }
        }
        return false;
    }
    @Override
    public int size(){
        return size;
    }
    @Override
    public Set<K> keySet(){
        Set<K> keySet=new HashSet<>();
        for(Collection<Node>bucket:buckets){
            for(Node node:bucket){
                keySet.add(node.key);
            }
        }
        return keySet;
    }
    @Override
    public V remove(K key) {
        int index = Math.floorMod(key.hashCode(), buckets.length);
        Collection<Node> bucket = buckets[index];

        for (Node node : bucket) {
            if (node.key.equals(key)) {
                V value = node.value;
                bucket.remove(node);
                size--;
                return value;
            }
        }
        return null;
    }
    @Override
    public V remove(K key, V value) {
        int index = Math.floorMod(key.hashCode(), buckets.length);
        Collection<Node> bucket = buckets[index];

        for (Node node : bucket) {
            if (node.key.equals(key) && node.value.equals(value)) {
                bucket.remove(node);
                size--;
                return value;
            }
        }
        return null;
    }
    @Override
    public Iterator<K> iterator() {
        return keySet().iterator();
    }
}
