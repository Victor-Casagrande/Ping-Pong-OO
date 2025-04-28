public class Ball {
    int x, y, dx, dy, lastTouch;
    private boolean bouncedOnServer = false;
    private boolean bouncedOnReceiver = false;
    private boolean justBounced = false;   // <<< nova flag

    public void resetBounces() {
        bouncedOnServer = bouncedOnReceiver = false;
    }
    public boolean hasBouncedOnServer()   { return bouncedOnServer; }
    public boolean hasBouncedOnReceiver() { return bouncedOnReceiver; }
    public void markBounceOnServer()      { bouncedOnServer = true; }
    public void markBounceOnReceiver()    { bouncedOnReceiver = true; }

    public boolean wasJustBounced()       { return justBounced; }   // <<< getter
    public void clearJustBounced()        { justBounced = false; }  // <<< limpa após render

    public void setPosition(int x, int y)   { this.x = x; this.y = y; }
    public void setDirection(int dx, int dy){ this.dx = dx; this.dy = dy; }
    public void setLastTouch(int t)         { this.lastTouch = t; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getLastTouch() { return lastTouch; }

    public void move() { x += dx; y += dy; }

    /* Checa se a bola quicou na rede ou saiu da tela.
     * Se quicou, marca o evento e retorna o tipo de evento.
     * Retorna:
     * 0 = sem evento
     * 1 = quicada válida
     * 2 = falta do servidor
     * 3 = falta do recebedor
     * 4 = excesso de quicadas
     */
    public int checkBounce(int midX) {
        if (y <= 0 || y >= Renderer.HEIGHT - 1) {
            if (!bouncedOnServer) {
                if (x < midX) {
                    markBounceOnServer();
                    justBounced = true;   // <<< sinaliza para render
                    return 1;
                } else {
                    return 2;
                }
            } else if (!bouncedOnReceiver) {
                if (x >= midX) {
                    markBounceOnReceiver();
                    justBounced = true;   // <<< sinaliza para render
                    return 1;
                } else {
                    return 3;
                }
            } else {
                return 4;
            }
        }
        return 0;
    }

    public void collidePaddle(Paddle p, Game g) {
        if (x == p.getX()+1 && y >= p.getY() && y < p.getY()+Renderer.PADDLE_HEIGHT) {
            int seg = y - p.getY();
            if (seg == 1 || seg == 2) g.setCurrentDelay(g.getBaseDelay() - 50);
            else g.setCurrentDelay(g.getBaseDelay() + 50);
            dx = -dx;
            setLastTouch(p.getX() < Renderer.WIDTH/2 ? 1 : 2);
            resetBounces();
        }
    }

    public void checkNet(Game g) {
        int mid = Renderer.WIDTH/2;
        if (x == mid) {
            double chance = 10 + (g.getSkill()-1)*8;
            double prob = lastTouch == 1
             ? chance
             : lastTouch == 2
             ? 100-chance
             : 0;
            if (g.getRandom().nextDouble()*100 < prob) {
                if (lastTouch == 1) g.increaseComputerScore();
                else  g.increasePlayerScore();
                g.spawnBall(lastTouch==1?2:1);
            }
        }
    }

    public boolean hits(Ability ab) {
        return ab.isActive() && x==ab.getX() && y==ab.getY();
    }
}
