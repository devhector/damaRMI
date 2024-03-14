import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JogoDamasRemote extends Remote {
	int registrarObserver(JogoDamasObserver observer) throws RemoteException;
	void removerObserver(JogoDamasObserver observer) throws RemoteException;
	int[][] obterEstadoTabuleiro() throws RemoteException;
	int movimentoValido(int fromRow, int fromCol, int toRow, int toCol) throws RemoteException;;
	int obterJogadorAtual() throws RemoteException;
	void iniciarEstadoTabuleiro(int[][] estadoCliente) throws RemoteException;
	void realizarMovimento(int jogador, int fromRow, int fromCol, int toRow, int toCol, boolean kill,boolean CPU) throws RemoteException;
}