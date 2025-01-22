package lockfree;

public class ThreadCount {

    //-Xms100G -Xmx100G -Xss1G
    //16352 thread 16k hard limit on os
    public static void main(String[] args) {
        int loopCount = 1000000;
        for(int i = 0; i < loopCount; i++) {
            new Thread(() -> {
                try {
                    System.out.println("Thread " + Thread.currentThread().getName() + " is running");
                    Thread.sleep(100000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }
}
