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

	private void trocarJogador() {
		System.out.println("trocarJogador");
		jogadorAtual = jogadorAtual == 1 ? 2 : 1;
	}

	@Override
	public int obterJogadorAtual() throws RemoteException {
		return jogadorAtual;
	}

	@Override
	public void realizarMovimento(int jogador, int fromRow, int fromCol, int toRow, int toCol, boolean kill) throws RemoteException {
		estadoTabuleiro[toRow][toCol] = estadoTabuleiro[fromRow][fromCol];
		estadoTabuleiro[fromRow][fromCol] = 0; // Limpar a posição antiga
		if (kill){
			int midRow = (fromRow + toRow) / 2;
			int midCol = (fromCol + toCol) / 2;
			estadoTabuleiro[midRow][midCol] = 0;
		}
		
		// Adicione a lógica de troca de turno, verificar vitoria, etc.
		for (int[] ints : estadoTabuleiro) {
			System.out.println(Arrays.toString(ints));
		}
		trocarJogador();
		notificarObservadores();
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
