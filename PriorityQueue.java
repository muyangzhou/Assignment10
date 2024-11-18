public class PriorityQueue<T extends Comparable<T>> {
        private static class Node<T> {
            T value;
            Node<T> next;
            int priority;

            Node(T value, int priority) {
                this.value = value;
                this.priority = priority;
                this.next = null;
            }
        }

        private Node<T> front;
        private int size;

        public priorityQueue() {
            this.front = null;
            size = 0;
        }

    public void enqueue(T value) {
        Node<T> current = front;
        Node<T> previous = null;

        //finds if value already exists within queue
        while (current != null) {
            if (current.value.equals(value)) {
                current.priority++;
                return;
            }
            previous = current;
            current = current.next;
        }

        //if not inserts new node w priority 1
        Node<T> newNode = new Node<>(value, 1);

        if (front == null || front.priority > newNode.priority) {
            newNode.next = front;
            front = newNode;
        } else {
            current = front;
            while (current.next != null && current.next.priority <= newNode.priority) {
                current = current.next;
            }
            newNode.next = current.next;
            current.next = newNode;
        }
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
                System.out.print("[" + current.value + ", " + current.priority + "] -> ");
                current = current.next;
            }
            System.out.println("null");
        }
    }
