public class PriorityQueueTester {
    public static void main(String[] args) {
        PriorityQueue314<Integer> q = new PriorityQueue314<Integer>();
        q.enqueue(5);
        q.enqueue(1);
        q.enqueue(8);
        q.enqueue(6);
        q.enqueue(-1);
        q.enqueue(8);
        q.print();
        System.out.println(q.size());
        while(q.size() != 0) {
            System.out.print(q.dequeue() + " ");
        }
        q.print();
    }
}
