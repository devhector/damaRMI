import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JogoDamasObserver extends Remote {
	void atualizarTabuleiro(int[][] novoEstado) throws RemoteException;
	void inicializarTabuleiro() throws RemoteException;
}
