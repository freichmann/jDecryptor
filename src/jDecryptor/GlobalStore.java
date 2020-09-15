/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2018
 */

package jDecryptor;

import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.Random;
import java.util.Set;

public class GlobalStore {
	private static GlobalStore _instance = null;
	private static final Set<Long> _covered = new HashSet<Long>();
	public static final Random _random = new Random();
	private Cryptor _bestCryptor = null;
	private Score _bestScore = null;
	private boolean _verbose;
	private static final Semaphore _scoreMutex = new Semaphore(1);
	private static final Semaphore _coveredMutex = new Semaphore(1);

	public static GlobalStore getInstance() {
		if (_instance == null)
			_instance = new GlobalStore();

		return _instance;
	}

	public boolean checkBest(final Cryptor iCryptor, final Score iScore, final String iName,
			final LoopCounter iLocalIterations) {
		try {
			_scoreMutex.acquire();
			if (_bestScore == null || iScore.compareTo(_bestScore) > 0) {
				_bestCryptor = new Cryptor(iCryptor);
				_bestScore = iScore;
				return true;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			_scoreMutex.release();
		}
		return false;
	}

	public void addCovered(Long iHash) {
		try {
			_coveredMutex.acquire();
			_covered.add(iHash);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			_coveredMutex.release();
		}
	}

	public boolean isCovered(Long iHash) {
		try {
			_coveredMutex.acquire();
			return _covered.contains(iHash);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			_coveredMutex.release();
		}
		return false;
	}

	public RatedCryptor getBest() {
		try {
			_scoreMutex.acquire();
			return new RatedCryptor(_bestCryptor, _bestScore);
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		finally {
			_scoreMutex.release();
		}
		return null;
	}

	public Score getBestScore() {
		try {
			_scoreMutex.acquire();
			return _bestScore;
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		finally {
			_scoreMutex.release();
		}
		return null;
	}

	public void setVerbose(boolean iBool) {
		_verbose=iBool;
	}

	public boolean getVerbose() {
		return _verbose;
	}
}