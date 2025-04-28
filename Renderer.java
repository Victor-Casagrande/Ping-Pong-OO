import java.util.Arrays;
import java.util.List;

public class Renderer {
    public static final int WIDTH = 40;
    public static final int HEIGHT = 20;
    public static final int PADDLE_HEIGHT = 4;
    public static final int NUM_ABILITIES = 3;
    public static final int MAX_BALLS = 50;

    private final char[][] prev = new char[HEIGHT][WIDTH];
    private final char[][] cur = new char[HEIGHT][WIDTH];

    public void drawStaticBoard() {
        System.out.print("\033[2J\033[H");
        for (int i = 0; i < WIDTH + 2; i++) System.out.print('#');
        System.out.println();
        for (int y = 0; y < HEIGHT; y++) {
            System.out.print('#');
            for (int x = 0; x < WIDTH; x++)
                System.out.print((x == WIDTH/2 && y % 2 == 0) ? '|' : ' ');
            System.out.println('#');
        }
        for (int i = 0; i < WIDTH + 2; i++) System.out.print('#');
        System.out.println();
    }

    public void updateDynamic(
      Paddle p, Paddle c,
      List<Ball> balls, List<Ability> abs,
      int sp, int sc, int pp, int pc
    ) {
        // limpa o buffer atual
        for (int y = 0; y < HEIGHT; y++) Arrays.fill(cur[y], ' ');

        // rede
        for (int y = 0; y < HEIGHT; y++)
            if (y % 2 == 0) cur[y][WIDTH/2] = '|';

        // raquetes
        for (int i = 0; i < PADDLE_HEIGHT; i++) {
            cur[p.getY() + i][p.getX()] = '|';
            cur[c.getY() + i][c.getX()] = '|';
        }

        // bolas (use 'X' se acabou de quicar)
        for (Ball b : balls) {
            char glyph = b.wasJustBounced() ? 'X' : 'O';
            cur[b.getY()][b.getX()] = glyph;
            b.clearJustBounced();  // volta ao normal no próximo frame
        }

        // habilidades
        for (Ability a : abs)
            if (a.isActive())
                cur[a.getY()][a.getX()] = a.getType();

        // imprime só os diffs
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (cur[y][x] != prev[y][x]) {
                    System.out.print(String.format("\033[%d;%dH%c", y+2, x+2, cur[y][x]));
                    prev[y][x] = cur[y][x];
                }
            }
        }

        // placar
        System.out.print(String.format(
          "\033[%d;1HSets: %d x %d   Pontos: %d x %d",
          HEIGHT+3, sp, sc, pp, pc
        ));
    }

    public void clearBelow() {
        System.out.print(String.format("\033[%d;1H", HEIGHT+5));
    }
}
