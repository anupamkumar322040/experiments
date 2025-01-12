package lockfree;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

public class LockFreeConcurrentQueue {
    AtomicReference<Node> head, tail;
    AtomicInteger size;

    public LockFreeConcurrentQueue() {
        head = new AtomicReference<>(null);
        tail = new AtomicReference<>(null);
        size = new AtomicInteger(0);
    }

    public void enqueue(String value) {
        Node newNode = new Node(value);
        Node currentTail;
        do {
            currentTail = tail.get();
        }while (!tail.compareAndSet(currentTail, newNode));
        head.compareAndSet(null, newNode);
        size.incrementAndGet();
    }

    public String dequeue() {
        Node currentHead;
        Node currentTail;
        do {
            currentHead = head.get();
            currentTail = tail.get();
            if (currentHead == null) {
                return "EMPTY";
            }
        } while (currentHead != currentTail && !head.compareAndSet(currentHead, currentHead.next));
        //for the last element
        if (currentHead == currentTail) {
            tail.compareAndSet(currentTail, null);
        }
        size.decrementAndGet();
        return currentHead.value;
    }

    static class Node {
        volatile String value;
        volatile Node next;

        Node(String value) {
            this.value = value;
        }
    }


    public static void main(String[] args) {
        AtomicLong counter = new AtomicLong();
        long loop = 100;//10000000;
        LockFreeConcurrentQueue queue = new LockFreeConcurrentQueue();
        Instant starts = Instant.now();
        Random random = new Random();
        CompletableFuture<Void> c1 = CompletableFuture.allOf(LongStream.range(0, loop).parallel().mapToObj(i -> CompletableFuture.supplyAsync(() -> CompletableFuture.supplyAsync(() -> {
            if (random.nextBoolean()) {
                queue.enqueue("t1-"+i);
            } else {
                String val = queue.dequeue();
                if (val.equals("EMPTY")) {
                    counter.incrementAndGet();
                }
            }
            return null;
        }))).toArray(CompletableFuture[]::new));

        CompletableFuture.allOf(c1).join();
        Instant ends = Instant.now();
        System.out.println("time to execute: " + Duration.between(starts, ends) + " total empty: " + counter.get() + " queue size: " + queue.size.get());
    }

}
