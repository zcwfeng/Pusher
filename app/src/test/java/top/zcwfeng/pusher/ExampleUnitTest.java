package top.zcwfeng.pusher;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);

        ByteBuffer  buffer =  ByteBuffer.allocate(2);
        buffer.get();
        buffer.get();
//        buffer.get();
    }
}