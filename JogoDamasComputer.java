import javax.swing.*;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Random;

public class JogoDamasComputer extends JogoDamasCliente {
	private final Random random = new Random();

	public JogoDamasComputer() {
		super();
		setTitle("Jogo de Damas - Computador");
	}

	@Override
	protected void iniciarLoopDePolling() {
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

				if (jogador == jogadorAtual) {
					fazerMovimentoAutomatico();
				}

				Thread.sleep(100);
			} catch (RemoteException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void fazerMovimentoAutomatico() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
        boolean moveMade = false;
    
        for (int i = 0; i < TAMANHO_TABULEIRO && !moveMade; i++) {
            for (int j = 0; j < TAMANHO_TABULEIRO && !moveMade; j++) {
                if (estadoTabuleiro[i][j] == jogador) {
                    int[] dx = {-1, 1};
                    int[] dy = {-1, 1};
                    boolean CPU = true;
    
                    for (int di : dx) {
                        for (int dj : dy) {
                            //TODO precisa fazer ele comer mais de 1 peça também
                            int toRow = i + di;
                            int toCol = j + dj;

                            if (toRow >= 0 && toRow < TAMANHO_TABULEIRO && toCol >= 0 && toCol < TAMANHO_TABULEIRO) {
                                try {
                                    jogadorAtual = remote.obterJogadorAtual();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                                 
                                if(jogador == jogadorAtual){
                                    try {
                                        int enable = remote.movimentoValido(i, j, toRow, toCol);
                                        if (enable != 0 ) {
                                            try {
                                                boolean kill = false;
                                                //seleciona tipo de jogada
                                                if (enable == 1) {
                                                    System.out.println("Movimento válido de: " + i + ", " + j + " para " + toRow + ", " + toCol);
                                                    remote.realizarMovimento(jogador, i, j, toRow, toCol, kill,CPU);
                                                }
                                                if(enable == 2){
                                                    kill = true;
                                                    System.out.println("Movimento válido de: " + i + ", " + j + " para " + toRow + ", " + toCol);
                                                    remote.realizarMovimento(jogador, i, j, toRow, toCol, kill,CPU);
                                                }
                                                moveMade = true;
                                                return;
                                            } catch (RemoteException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                    
                            }
                            }
                        }
                    }
                }
            }
        }
    }
    

	public static void main(String[] args) {
		SwingUtilities.invokeLater(JogoDamasComputer::new);
	}
}
