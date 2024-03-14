import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JogoDamasServer extends UnicastRemoteObject implements JogoDamasRemote {
	private int[][] estadoTabuleiro;
	private int jogadores = 0;
	private int jogadorAtual = 1;
	private List<JogoDamasObserver> observadores;
	private final int TAMANHO_TABULEIRO = 8;

	public JogoDamasServer() throws RemoteException {
		estadoTabuleiro = new int[TAMANHO_TABULEIRO][TAMANHO_TABULEIRO];
		observadores = new ArrayList<>();
	}

	@Override
	public int registrarObserver(JogoDamasObserver observer) throws RemoteException {
		observadores.add(observer);
		if (jogadores < 3) {
			System.out.println("Jogador " + (jogadores + 1) + " registrado.");
			return ++jogadores;
		}
		return -1;
	}

	@Override
	public void removerObserver(JogoDamasObserver observer) throws RemoteException {
		observadores.remove(observer);
	}

	@Override
	public int[][] obterEstadoTabuleiro() throws RemoteException {
		System.out.println("obterEstadoTabuleiro");
		return estadoTabuleiro;
	}

	@Override
	public void iniciarEstadoTabuleiro(int[][] estadoCliente) throws RemoteException {
		System.out.println("iniciarEstadoTabuleiro");
		estadoTabuleiro = estadoCliente;
		notificarObservadores();
	}

	private void notificarObservadores() {
		System.out.println("notificarObservadores");
		try {
			for (JogoDamasObserver observer : observadores) {
				observer.atualizarTabuleiro(estadoTabuleiro);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void reiniciarJogo() throws RemoteException {
		for (int i = 0; i < TAMANHO_TABULEIRO; i++) {
			for (int j = 0; j < TAMANHO_TABULEIRO; j++) {
				estadoTabuleiro[i][j] = 0;
			}
		}
		System.out.println("Jogo reiniciado.");
		for (JogoDamasObserver observer : observadores) {
			observer.inicializarTabuleiro();
		}
	}
	
	private void trocarJogador(boolean kill) throws RemoteException {
		if (!existemMovimentosValidos(jogadorAtual) || contarCaracteres(estadoTabuleiro, jogadorAtual) == 0) {
			System.out.println("Vitória do jogador " + (jogadorAtual == 1 ? 2 : 1));
			reiniciarJogo();
			return;
		}
		if(kill){
			System.out.println("Permanece Jogador");
			return;
		}
		System.out.println("trocarJogador");
		jogadorAtual = jogadorAtual == 1 ? 2 : 1;
	}
	

	@Override
	public int obterJogadorAtual() throws RemoteException {
		return jogadorAtual;
	}

	@Override
	public void realizarMovimento(int jogador, int fromRow, int fromCol, int toRow, int toCol, boolean kill,boolean CPU) throws RemoteException {
		estadoTabuleiro[toRow][toCol] = estadoTabuleiro[fromRow][fromCol];
		estadoTabuleiro[fromRow][fromCol] = 0; // Limpar a posição antiga

		// trata peça morta
		if (kill){
			int midRow = (fromRow + toRow) / 2;
			int midCol = (fromCol + toCol) / 2;
			estadoTabuleiro[midRow][midCol] = 0; 
		}
		
		// TODO verificar vitoria, etc.
		for (int[] ints : estadoTabuleiro) {
			System.out.println(Arrays.toString(ints));
		}

		trocarJogador(kill);
		notificarObservadores();
	}

	public boolean existemMovimentosValidos(int jogador) {
		for (int i = 0; i < TAMANHO_TABULEIRO; i++) {
			for (int j = 0; j < TAMANHO_TABULEIRO; j++) {
				if (estadoTabuleiro[i][j] == jogador) {
					// Verifique todas as direções possíveis para um movimento válido
					for (int di = -1; di <= 1; di++) {
						for (int dj = -1; dj <= 1; dj++) {
							int toRow = i + di;
							int toCol = j + dj;
							if (toRow >= 0 && toRow < TAMANHO_TABULEIRO && toCol >= 0 && toCol < TAMANHO_TABULEIRO) {
								if (movimentoValido(i, j, toRow, toCol) != 0) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	//Verifica se o movimento a ser realiazado é valido
	public int movimentoValido(int fromRow, int fromCol, int toRow, int toCol) {
		//calculo para descobrir o deslocameto da peça
		int deltaX = toCol - fromCol;
		int deltaY = toRow - fromRow;
		
		//impede a peça de voltar
		if ((estadoTabuleiro[fromRow][fromCol] == 1 && toRow < fromRow || estadoTabuleiro[fromRow][fromCol] == 2 && toRow > fromRow)){
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
				// Verifica se a célula intermediária contém uma peça do oponente
				if (estadoTabuleiro[midRow][midCol] != jogadorAtual && estadoTabuleiro[midRow][midCol] != 0) {
					return 2; 
				}
			}
		}
		return 0; 
	}
	public int contarCaracteres(int[][] matriz, int caractere) {
        int contagem = 0;
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[i].length; j++) {
                if (matriz[i][j] == caractere) {
                    contagem++;
                }
            }
        }
        return contagem;
    }

	public static void main(String[] args) {
		try {
			JogoDamasRemote jogoDamas = new JogoDamasServer();
			java.rmi.registry.LocateRegistry.createRegistry(1099);
			java.rmi.registry.LocateRegistry.getRegistry().rebind("JogoDamas", jogoDamas);
			System.out.println("Servidor JogoDamas pronto.");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	
}

