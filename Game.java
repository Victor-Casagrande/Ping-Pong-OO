import java.io.IOException;
import java.util.*;

public class Game {
    // parâmetros de menu
    private int baseDelay, currentDelay;
    private int skill, targetSets, pointsPerSet, abilityFreq;

    // entidades do jogo
    private Paddle player, computer;
    private final List<Ball> balls = new ArrayList<>();
    private final List<Ability> abilities = new ArrayList<>();
    private final Renderer renderer = new Renderer();
    private final Random rand = new Random();

    // placar
    private int scorePlayer = 0, scoreComputer = 0;
    private int setsPlayer = 0, setsComputer = 0;

    // flags de input
    private volatile boolean upPressed = false;
    private volatile boolean downPressed = false;
    private volatile boolean quitPressed = false;

    public static void main(String[] args) {
        new Game().init();
    }

    private void startInputThread() {
        Thread t = new Thread(() -> {
            try {
                var console = System.console();
                var reader = (console != null)
                    ? console.reader()
                    : new java.io.InputStreamReader(System.in);
                while (!quitPressed) {
                    int ch = reader.read();
                    if (ch < 0) break;
                    char c = Character.toLowerCase((char) ch);
                    switch (c) {
                        case 'w' -> upPressed = true;
                        case 's' -> downPressed = true;
                        case 'q' -> quitPressed = true;
                    }
                }
            } catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    public void init() {
        Scanner sc = new Scanner(System.in); // System.in não é fechado
        System.out.print("Velocidade base (ms): ");
        baseDelay = sc.nextInt();
        currentDelay = baseDelay;
        System.out.print("Dificuldade (1-10): ");
        skill = sc.nextInt();
        System.out.print("Sets para vencer: ");
        targetSets = sc.nextInt();
        System.out.print("Pontos por set: ");
        pointsPerSet = sc.nextInt();
        System.out.print("Freq. hab. (s): ");
        abilityFreq = sc.nextInt();

        player = new Paddle(1, Renderer.HEIGHT/2 - Renderer.PADDLE_HEIGHT/2);
        computer = new Paddle(Renderer.WIDTH-2, Renderer.HEIGHT/2 - Renderer.PADDLE_HEIGHT/2);

        spawnBall(rand.nextBoolean() ? 1 : 2);
        initAbilities();

        renderer.drawStaticBoard();
        startInputThread();
        gameLoop();
    }

    private void gameLoop() {
        long lastAbility = System.currentTimeMillis();
        while (!quitPressed && setsPlayer < targetSets && setsComputer < targetSets) {
            // input não‐bloqueante
            if (upPressed)   { player.moveUp();    upPressed   = false; }
            if (downPressed) { player.moveDown();  downPressed = false; }
            if (quitPressed) break;

            // lógica principal
            update();

            // power-ups renascem de tempo em tempo
            if (System.currentTimeMillis() - lastAbility >= abilityFreq * 1000L) {
                abilities.forEach(Ability::respawn);
                lastAbility = System.currentTimeMillis();
            }

            // render incremental
            renderer.updateDynamic(
              player, computer, balls, abilities, setsPlayer, setsComputer, scorePlayer, scoreComputer
            );

            // atraso do frame
            try { Thread.sleep(currentDelay); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }

        renderer.clearBelow();
        if (setsPlayer >= targetSets) System.out.println("Parabéns, você venceu!");
        else if (quitPressed) System.out.println("Jogo encerrado pelo jogador.");
        else System.out.println("Fim de jogo: " + setsPlayer + " x " + setsComputer);
    }

    private void update() {
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball b = it.next();
            b.move();

            // trata quicadas na mesa conforme a ordem server→receiver
            int ev = b.checkBounce(Renderer.WIDTH / 2);
            if (ev == 1) {
                // quicada válida: inverte levemente dy para simular salto
                b.setDirection(b.dx, -b.dy);
            } else if (ev == 2) {
                scoreComputer++;
                it.remove();
                checkSetAndSpawn(2);
                return;
            } else if (ev == 3) {
                scorePlayer++;
                it.remove();
                checkSetAndSpawn(1);
                return;
            } else if (ev == 4) {
                // excesso de quicadas: ponto para quem não errou
                if (b.hasBouncedOnReceiver()) scorePlayer++;
                else scoreComputer++;
                it.remove();
                checkSetAndSpawn(b.hasBouncedOnServer() ? 2 : 1);
                return;
            }

            // colisões normais
            b.collidePaddle(player, this);
            b.collidePaddle(computer, this);
            b.checkNet(this);

            // se escapar pelos lados
            if (b.getX() <= 0) {
                scoreComputer++;
                it.remove();
                checkSetAndSpawn(2);
                return;
            }
            if (b.getX() >= Renderer.WIDTH - 1) {
                scorePlayer++;
                it.remove();
                checkSetAndSpawn(1);
                return;
            }

            // power-ups
            for (Ability ab : abilities) {
                if (ab.isActive() && b.hits(ab)) {
                    ab.apply(b, this);
                }
            }
        }

        // Adversario: segue a bola principal
        if (!balls.isEmpty()) {
            Ball lead = balls.get(0);
            if (computer.getY() + Renderer.PADDLE_HEIGHT/2 < lead.getY())
                computer.moveDown();
            else if (computer.getY() + Renderer.PADDLE_HEIGHT/2 > lead.getY())
                computer.moveUp();
        }
    }

    // Checa se atingiu pointsPerSet para virar set e spawna nova bola
    private void checkSetAndSpawn(int serverWinner) {
        if (scorePlayer >= pointsPerSet) {
            setsPlayer++;
            scorePlayer = scoreComputer = 0;
        }
        if (scoreComputer >= pointsPerSet) {
            setsComputer++;
            scorePlayer = scoreComputer = 0;
        }
        // só spawna se ainda não alcançou targetSets
        if (setsPlayer < targetSets && setsComputer < targetSets) {
            spawnBall(serverWinner);
        }
    }

    public void spawnBall(int server) {
        balls.clear();
        Ball b = new Ball();
        if (server == 1) {
            b.setPosition(3, player.getY() + Renderer.PADDLE_HEIGHT/2);
            b.setDirection(1, rand.nextInt(3) - 1);
        } else {
            b.setPosition(Renderer.WIDTH-4, computer.getY() + Renderer.PADDLE_HEIGHT/2);
            b.setDirection(-1, rand.nextInt(3) - 1);
        }
        b.setLastTouch(0);
        b.resetBounces();
        balls.add(b);
        currentDelay = baseDelay;
    }

    private void initAbilities() {
        abilities.clear();
        for (int i = 0; i < Renderer.NUM_ABILITIES; i++) {
            Ability ab = new Ability();
            ab.respawn();
            abilities.add(ab);
        }
    }

    // acessores para Ball/Ability
    public int getSkill()              { return skill; }
    public Random getRandom()          { return rand; }
    public int getBaseDelay()          { return baseDelay; }
    public void setCurrentDelay(int d) { currentDelay = Math.max(50, Math.min(d, baseDelay)); }
    public void increasePlayerScore()  { scorePlayer++; }
    public void increaseComputerScore(){ scoreComputer++; }
    public int getBallCount()          { return balls.size(); }
    public void addBall(Ball b)        { balls.add(b); }
}
