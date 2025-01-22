package lockfree;


import java.io.FileOutputStream;

//10232 max file descriptors
public class FileDescriptor {
    public static void main(String[] args) {
        int loopCount = 2;
        int count = 0;
        try {
            for (int i = 0; i < loopCount; i++) {
                FileOutputStream geek_out = new FileOutputStream("anu/FILE"+i+".txt");
                count = i;
            }
        } catch (Exception e) {
            System.out.println("Number of open fd: " + count);
            e.printStackTrace();
        }

    }
}
