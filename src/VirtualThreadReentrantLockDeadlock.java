import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VirtualThreadReentrantLockDeadlock {
    public static void main(String[] args) {
        final boolean shouldPin = args.length == 0 || Boolean.parseBoolean(args[0]);
        final ReentrantLock lock = new ReentrantLock(true); // With faireness to ensure that the unpinned thread is next in line

        lock.lock();

        Runnable takeLock = () -> {
            try {
                System.out.println(Thread.currentThread() + " waiting for lock");
                lock.lock();
                System.out.println(Thread.currentThread() + " took lock");
            } finally {
                lock.unlock();
                System.out.println(Thread.currentThread() + " released lock");
            }
        };

        Thread unpinnedThread = Thread.ofVirtual().name("unpinned").start(takeLock);

        List<Thread> pinnedThreads = IntStream.range(0, Runtime.getRuntime().availableProcessors())
                .mapToObj(i -> Thread.ofVirtual().name("pinning-" + i).start(() -> {
                    if (shouldPin) {
                        synchronized (new Object()) {
                            takeLock.run();
                        }
                    } else {
                        takeLock.run();
                    }
                })).toList();

        lock.unlock();

        Stream.concat(Stream.of(unpinnedThread), pinnedThreads.stream()).forEach(thread -> {
            try {
                if (!thread.join(Duration.ofSeconds(3))) {
                    throw new RuntimeException("Deadlock detected");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
