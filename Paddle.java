public class Paddle {
    private final int x;
    private int y;

    public Paddle(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void moveUp() {
        if (y > 0) y--;
    }

    public void moveDown() {
        if (y + Renderer.PADDLE_HEIGHT < Renderer.HEIGHT) y++;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
