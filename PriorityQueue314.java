/**
 * A PriorityQueue class that implements consistent placement for equivalent
 * objects
 */
public class PriorityQueue314<T extends Comparable<T>> {
    /**
     * Node class that allows LinkedList-style implementation of the PQ314
     */
    private static class Node<T> {
        T value;
        Node<T> next;

        /**
         * @param value value to be stored by this Node
         */
        Node(T value) {
            this.value = value;
            this.next = null;
        }
    }

    private Node<T> front;
    private int size;

    /**
     * Default constructor of PQ314 that creates empty PQ314
     */
    public PriorityQueue314() {
        this.front = null;
        size = 0;
    }

    /**
     * Adds value to end of the queue
     * @param value value to add to end of the queue
     */
    public void enqueue(T value) {
        // if not inserts new node w priority 1
        Node<T> newNode = new Node<>(value);

        if (front == null || front.value.compareTo(newNode.value) > 0) {
            newNode.next = front;
            front = newNode;
        } else {
            Node<T> current = front;

            while (current.next != null && current.next.value.compareTo(newNode.value) <= 0) {
                current = current.next;
            }
            newNode.next = current.next;
            current.next = newNode;
        }

        size++;
    }

    /**
     * Retrieves and removes the value at the front of the queue
     * @return the value at the front of the queue
     */
    public T dequeue() {
        if (front == null) {
            throw new IllegalStateException("front is null");
        }
        T value = front.value;
        front = front.next;
        size--;
        return value;
    }

    /**
     * Retrives the value at the front of the queue
     * @return the value at the front of the queue
     */
    public T peek() {
        if (front == null) {
            throw new IllegalStateException("front is null");
        }
        return front.value;
    }

    /**
     * @return size of the queue
     */
    public int size() {
        return size;
    }

    /**
     * Prints the queue in format [data] -> [data] -> ... -> null
     * For debugging purposes
     */
    public void print() {
        Node<T> current = front;
        while (current != null) {
            System.out.print("[" + current.value + "] -> ");
            current = current.next;
        }
        System.out.println("null");
    }
}
