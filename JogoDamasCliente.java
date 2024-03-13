import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class JogoDamasCliente extends JFrame implements JogoDamasObserver{
	private final int TAMANHO_TABULEIRO = 8;
	private final JButton[][] tabuleiroBotoes = new JButton[TAMANHO_TABULEIRO][TAMANHO_TABULEIRO];
	private int[][] estadoTabuleiro = new int[TAMANHO_TABULEIRO][TAMANHO_TABULEIRO];
	private boolean keepPolling = true;
	private final Icon peca1 = new ImageIcon("peca1.png");
	private final Icon peca2 = new ImageIcon("peca2.png");
	private JogoDamasRemote remote;
	private int jogador = -1;
	private int jogadorAtual = -1;
	private int selectedRow = -1;
	private int selectedCol = -1;

	public JogoDamasCliente() {
		setTitle("Jogo de Damas");
		setSize(600, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		inicializarTabuleiro();
		addActions();

		setVisible(true);

		conectarRMI();
		new Thread(this::iniciarLoopDePolling).start();
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
	

	private void inicializarTabuleiro() {
		setLayout(new GridLayout(TAMANHO_TABULEIRO, TAMANHO_TABULEIRO));

		for (int i = 0; i < TAMANHO_TABULEIRO; i++) {
			for (int j = 0; j < TAMANHO_TABULEIRO; j++) {
				final int row = i;
				final int col = j;
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

				button.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						super.mouseClicked(e);
						handlePieceClick(row, col);
					}
				});

				tabuleiroBotoes[i][j] = button;
				add(button);
			}
		}
	}

	private void addActions() {
		for (int i = 0; i < TAMANHO_TABULEIRO; i++) {
			for (int j = 0; j < TAMANHO_TABULEIRO; j++) {
				final int row = i;
				final int col = j;

				tabuleiroBotoes[i][j].addActionListener(actionEvent -> {
                    try {
                        handleTableClick(row, col);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });
			}
		}
	}

	private void handlePieceClick(int row, int col) {
		if (estadoTabuleiro[row][col] == jogador && jogador == jogadorAtual) {
			selectedRow = row;
			selectedCol = col;
			System.out.println("jogadorAtual: " + jogador + " clicou na peça de posição: " + row + ", " + col);
		}
	}

	private void handleTableClick(int row, int col) throws RemoteException {
		
		if (selectedRow != -1 && selectedCol != -1) {
			boolean kill = false;
			int enable = movimentoValido(selectedRow, selectedCol, row, col);
			//seleciona tipo de jogada
			if (enable == 1) {
				System.out.println("Movimento válido de: " + selectedRow + ", " + selectedCol + " para " + row + ", " + col);
				remote.realizarMovimento(jogador, selectedRow, selectedCol, row, col, kill);
			}
			// TODO precisa de loop caso um jogador coma uma peça
			if(enable == 2){
				kill = true;
				System.out.println("Movimento válido de: " + selectedRow + ", " + selectedCol + " para " + row + ", " + col);
				remote.realizarMovimento(jogador, selectedRow, selectedCol, row, col, kill);
			}
			selectedRow = -1;
			selectedCol = -1;
		}
	}

	//Verifica se o movimento a ser realiazado é valido
	private int movimentoValido(int fromRow, int fromCol, int toRow, int toCol) {
		//calculo para descobrir o deslocameto da peça
		int deltaX = toCol - fromCol;
		int deltaY = toRow - fromRow;
		
		//impede a peça de voltar
		if ((estadoTabuleiro[fromRow][fromCol] == 1 && toRow < fromRow)){
			return 0;
		}
		if (estadoTabuleiro[fromRow][fromCol] == 2 && toRow > fromRow){
			return 0;
		}
		//movimento simples
		if (Math.abs(deltaX) == 1 && Math.abs(deltaY) == 1) {
			if (estadoTabuleiro[toRow][toCol] == 0) {
				return 1; 
			} 
		}
		//salto movimento de 2 casas
		if (Math.abs(deltaX) == 2 && Math.abs(deltaY) == 2) {
			if (estadoTabuleiro[toRow][toCol] == 0) {
				int midRow = (fromRow + toRow) / 2;
				int midCol = (fromCol + toCol) / 2;
				// Verificar se a célula intermediária contém uma peça do oponente
				if (estadoTabuleiro[midRow][midCol] != jogador && estadoTabuleiro[midRow][midCol] != 0) {
					return 2; 
				}
			}
		}
		return 0; 
	}


	private void verificarVitoria() {
		// Adicione a lógica de verificação de vitória aqui
		// Este é um exemplo básico, você precisa implementar as regras completas do jogo de damas
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
		SwingUtilities.invokeLater(JogoDamasCliente::new);
	}
}