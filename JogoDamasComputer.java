import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class JogoDamasComputer extends JFrame implements JogoDamasObserver{
    private final int TAMANHO_TABULEIRO = 8;
    private final JButton[][] tabuleiroBotoes = new JButton[TAMANHO_TABULEIRO][TAMANHO_TABULEIRO];
    private int[][] estadoTabuleiro = new int[TAMANHO_TABULEIRO][TAMANHO_TABULEIRO];
    private boolean keepPolling = true;
    private final Icon peca1 = new ImageIcon("./peca1.png");
    private final Icon peca2 = new ImageIcon("./peca2.png");
    private JogoDamasRemote remote;
    private int jogador = -1;
    private int jogadorAtual = -1;
    private int selectedRow = -1;
    private int selectedCol = -1;

    public JogoDamasComputer() {
        setTitle("Jogo de Damas");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        inicializarTabuleiro();

        setVisible(true);

        conectarRMI();
        new Thread(this::iniciarLoopDePolling).start();
        new Thread(this::fazerJogada).start();
    }

    private void conectarRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            remote = (JogoDamasRemote) registry.lookup("JogoDamas");
            jogador = remote.registrarObserver(this);
            jogadorAtual = remote.obterJogadorAtual();
            remote.iniciarEstadoTabuleiro(estadoTabuleiro);
            atualizarTabuleiro();
            System.out.println("Jogador " + jogador + " conectado.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void atualizarTabuleiro(int[][] novoEstado) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            System.out.println("atualizarTabuleiro");
            estadoTabuleiro = novoEstado;
            atualizarTabuleiro();
            obterJogadorAtual();
        });
    }

    private void obterJogadorAtual() {
        System.out.println("obterJogadorAtual");
        try {
            jogadorAtual = remote.obterJogadorAtual();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    //Inicializa o tabuleiro do jogo.
    private void inicializarTabuleiro() {
        setLayout(new GridLayout(TAMANHO_TABULEIRO, TAMANHO_TABULEIRO));

        for (int i = 0; i < TAMANHO_TABULEIRO; i++) {
            for (int j = 0; j < TAMANHO_TABULEIRO; j++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(50, 50));
                button.setBackground((i + j) % 2 == 0 ? Color.WHITE : Color.BLACK);

                if ((i + j) % 2 != 0) {
                    if (i < 3) {
                        estadoTabuleiro[i][j] = 1;
                        button.setIcon(peca1);
                    } else if (i > 4) {
                        estadoTabuleiro[i][j] = 2;
                        button.setIcon(peca2);
                    }
                }
                tabuleiroBotoes[i][j] = button;
                add(button);
            }
        }
    }

    //verifica se o movimento feito é valido
    private boolean movimentoValido(int fromRow, int fromCol, int toRow, int toCol) {
        return Math.abs(toRow - fromRow) == 1 && Math.abs(toCol - fromCol) == 1 &&
                estadoTabuleiro[toRow][toCol] == 0;
    }

    private void verificarVitoria() {
        // Adicione a lógica de verificação de vitória aqui
        // Este é um exemplo básico, você precisa implementar as regras completas do jogo de damas
    }

    private void realizarMovimento(int fromRow, int fromCol, int toRow, int toCol, boolean kill) {
        try {
            remote.realizarMovimento(jogador, fromRow, fromCol, toRow, toCol, kill);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void fazerJogada() {
        while (keepPolling) {
            if (jogador == jogadorAtual) {
                System.out.println("Jogador " + jogador + " é o próximo a jogar.");
                // Lógica da heurística simples: mover para a posição com mais peças adversárias
                int[] melhorMovimento = obterMelhorMovimento();
                if (melhorMovimento != null) {
                    System.out.println("Melhor movimento: " + Arrays.toString(melhorMovimento));
                    int fromRow = melhorMovimento[0];
                    int fromCol = melhorMovimento[1];
                    int toRow = melhorMovimento[2];
                    int toCol = melhorMovimento[3];
                    boolean kill = false;

                    if(movimentoValido(fromRow, fromCol, toRow, toCol)){
                        
                        System.out.println("Movimento válido de: " + fromRow + ", " + fromCol + " para " + toRow + ", " + toCol);
                        realizarMovimento(fromRow, fromCol, toRow, toCol, kill);
                    }
                }
            }
        }
    }

    private int[] obterMelhorMovimento() {
        int[] melhorMovimento = null;
        int maxPecasAdversarias = 0;
        
        
        for (int i = 0; i < TAMANHO_TABULEIRO; i++) {
            for (int j = 0; j < TAMANHO_TABULEIRO; j++) {
                if (estadoTabuleiro[i][j] == jogador) {
                    
                    // Verificar movimentos possíveis para a peça atual
                    for (int row = 0; row < TAMANHO_TABULEIRO; row++) {
                        for (int col = 0; col < TAMANHO_TABULEIRO; col++) {
                            if (movimentoValido(i, j, row, col)) {
                                int[][] estadoTemporario = clonarEstadoTabuleiro();
                                estadoTemporario[row][col] = jogador;
                                estadoTemporario[i][j] = 0; // Remover peça da posição original

                                // Avaliar o número de peças adversárias no novo estado
                                int pecasAdversarias = contarPecasAdversarias(estadoTemporario);
                                if (pecasAdversarias > maxPecasAdversarias) {
                                    maxPecasAdversarias = pecasAdversarias;
                                    melhorMovimento = new int[]{i, j, row, col};
                                }
                            }
                        }
                    }
                }
            }
        }

        return melhorMovimento;
    }
 
    private int contarPecasAdversarias(int[][] estado) {
        int count = 0;
        for (int i = 0; i < TAMANHO_TABULEIRO; i++) {
            for (int j = 0; j < TAMANHO_TABULEIRO; j++) {
                if (estado[i][j] != jogador && estado[i][j] != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private int[][] clonarEstadoTabuleiro() {
        int[][] clone = new int[TAMANHO_TABULEIRO][TAMANHO_TABULEIRO];
        for (int i = 0; i < TAMANHO_TABULEIRO; i++) {
            clone[i] = Arrays.copyOf(estadoTabuleiro[i], TAMANHO_TABULEIRO);
        }
        return clone;
    }

    private void iniciarLoopDePolling() {
        while (keepPolling) {
            try {
                int[][] novoEstado = remote.obterEstadoTabuleiro();

                if (!Arrays.deepEquals(novoEstado, estadoTabuleiro)) {
                    SwingUtilities.invokeLater(() -> {
                        estadoTabuleiro = novoEstado;
                        atualizarTabuleiro();
                        obterJogadorAtual();
                    });
                }
                Thread.sleep(100);
            } catch (RemoteException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void atualizarTabuleiro() {
        for (int[] linha : estadoTabuleiro) {
            System.out.println(Arrays.toString(linha));
        }
        System.out.println("");
        for (int i = 0; i < TAMANHO_TABULEIRO; i++) {
            for (int j = 0; j < TAMANHO_TABULEIRO; j++) {
                JButton button = tabuleiroBotoes[i][j];

                int valor = estadoTabuleiro[i][j];
                if (valor == 1) {
                    button.setIcon(peca1);
                } else if (valor == 2) {
                    button.setIcon(peca2);
                } else {
                    button.setIcon(null);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(JogoDamasComputer::new);
    }
}