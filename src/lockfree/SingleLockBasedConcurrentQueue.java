package lockfree;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.LongStream;

public class SingleLockBasedConcurrentQueue {
    Lock lock = new ReentrantLock();

    static class Node {
        String value;
        Node next;

        Node(String value) {
            this.value = value;
        }
    }

    Node head;
    Node tail;

    public void enqueue(String value) {
       // System.out.println("ENQUEUE");
        lock.lock();
        Node newNode = new Node(value);
        if (head == null) {
            head = tail = newNode;
            lock.unlock();
            return;
        }
        tail.next = newNode;
        tail = newNode;
        lock.unlock();
    }

    public String dequeue() {
        //System.out.println("DEQUEUE");
        lock.lock();
        if (head == null) {
            lock.unlock();
            return "EMPTY";
        }
        String val = head.value;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        lock.unlock();
        return val;
    }

    public long size() {
        long size = 0;
        Node current = head;
        while (current != null) {
            size++;
            current = current.next;
        }
        return size;
    }

    public static void main(String[] args) {
        AtomicLong counter = new AtomicLong();
        long loop = 10000000;
        SingleLockBasedConcurrentQueue queue = new SingleLockBasedConcurrentQueue();
        Instant starts = Instant.now();
        Random random = new Random();
        CompletableFuture<Void> c1 = CompletableFuture.allOf(LongStream.range(0, loop).parallel().mapToObj(i -> CompletableFuture.supplyAsync(() -> CompletableFuture.supplyAsync(() -> {
            if (random.nextBoolean()) {
                queue.enqueue("t1-");
            } else {
                String val = queue.dequeue();
                if (val.equals("EMPTY")) {
                    counter.incrementAndGet();
                }
            }
            return null;
        }))).toArray(CompletableFuture[]::new));

//        CompletableFuture<Void> c2 = CompletableFuture.allOf(LongStream.range(0, loop).mapToObj(i -> CompletableFuture.supplyAsync(() -> CompletableFuture.supplyAsync(() -> {
//            queue.enqueue("t2-");
//            return null;
//        }))).toArray(CompletableFuture[]::new));
//
//        CompletableFuture<Void> c3 = CompletableFuture.allOf(LongStream.range(0, loop).mapToObj(i -> CompletableFuture.supplyAsync(() -> CompletableFuture.supplyAsync(() -> {
//            String val = queue.dequeue();
//            if (val.equals("EMPTY")) {
//                counter.incrementAndGet();
//            }
//            return null;
//        }))).toArray(CompletableFuture[]::new));
//        CompletableFuture<Void> c4 = CompletableFuture.allOf(LongStream.range(0, loop).mapToObj(i -> CompletableFuture.supplyAsync(() -> CompletableFuture.supplyAsync(() -> {
//            String val = queue.dequeue();
//            if (val.equals("EMPTY")) {
//                counter.incrementAndGet();
//            }
//            return null;
//        }))).toArray(CompletableFuture[]::new));
        CompletableFuture.allOf(c1).join();
        Instant ends = Instant.now();
        System.out.println("time to execute: " + Duration.between(starts, ends) + " total empty: " + counter.get() + " queue size: " + queue.size());
    }
}
