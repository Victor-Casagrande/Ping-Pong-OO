import java.util.Random;

public class Ability {
    private static final Random R = new Random();
    private int x, y;
    private char type;
    private boolean active;

    public void respawn() {
        x = 2 + R.nextInt(Renderer.WIDTH-4);
        y = 1 + R.nextInt(Renderer.HEIGHT-2);
        type = switch (R.nextInt(3)) {
            case 0 -> '+';
            case 1 -> '-';
            default -> '*';
        };
        active = true;
    }

    public void apply(Ball b, Game g) {
        if (b.getLastTouch() != 1) return;
        switch (type) {
            case '+' -> g.setCurrentDelay(g.getBaseDelay() - 20);
            case '-' -> g.setCurrentDelay(g.getBaseDelay() + 20);
            case '*' -> {
                int fac = 2 + g.getRandom().nextInt(4);
                for (int i = 1; i < fac && g.getBallCount() < Renderer.MAX_BALLS; i++) {
                    Ball nb = new Ball();
                    nb.setPosition(b.getX(), b.getY());
                    nb.setDirection(
                      g.getRandom().nextBoolean() ? 1 : -1,
                      g.getRandom().nextBoolean() ? 1 : -1
                    );
                    nb.setLastTouch(b.getLastTouch());
                    g.addBall(nb);
                }
            }
        }
        active = false;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public char getType() { return type; }
    public boolean isActive(){ return active; }
}
