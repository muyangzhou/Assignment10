public class PriorityQueue314<T extends Comparable<T>> {
    private static class Node<T> {
        T value;
        Node<T> next;

        Node(T value) {
            this.value = value;
            this.next = null;
        }
    }

    private Node<T> front;
    private int size;

    public PriorityQueue314() {
        this.front = null;
        size = 0;
    }

public void enqueue(T value) {
    //if not inserts new node w priority 1
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

    public T dequeue() {
        if (front == null) {
            throw new IllegalStateException("front is null");
        }
        T value = front.value;
        front = front.next;
        size--;
        return value;
    }

    public T peek() {
        if (front == null) {
            throw new IllegalStateException("front is null");
        }
        return front.value;
    }

    public int size() {
        return size;
    }

    public void print() {
        Node<T> current = front;
        while (current != null) {
            System.out.print("[" + current.value + "] -> ");
            current = current.next;
        }
        System.out.println("null");
    }
}
