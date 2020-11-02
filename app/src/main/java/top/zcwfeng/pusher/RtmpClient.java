package top.zcwfeng.pusher;

public class RtmpClient {

    static {
        System.loadLibrary("native-lib");
    }

    private final int fps;
    private final int bitRate;
    private final int width;
    private final int height;

    public RtmpClient(int width, int height, int fps, int bitRate) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bitRate = bitRate;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
