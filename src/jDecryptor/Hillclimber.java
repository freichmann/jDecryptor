/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2020
 */

package jDecryptor;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;

import jDecryptor.Move.CharMove;
import jDecryptor.Move.Move;
import jDecryptor.Move.SymbolMove;

public class Hillclimber implements Callable<LoopCounter> {

	private final Set<LanguageStatistics> _letterSequences;
	private final Cipher _cipher;
	private final String _name;
	private final Double _randomizeFraction;
	private final Integer _maximumLoops;
	private final LoopCounter _loops = new LoopCounter();
	private final Set<String> _alphabet;

	public Hillclimber(final String iName, final Cipher iCipher, final Integer iMaximumLoops,
			final Set<LanguageStatistics> iLetterSequences, final Set<String> iAlphabet, final Double iRandomizeFraction) throws Exception {
		_letterSequences = iLetterSequences;
		_name = iName;
		_randomizeFraction = iRandomizeFraction;
		_cipher = iCipher;
		_maximumLoops = iMaximumLoops;
		_alphabet = iAlphabet;
	}

	@Override
	public LoopCounter call() throws Exception {
		while (_maximumLoops==0 || _maximumLoops>_loops._resets) {
			_loops._resets++;
			_loops._optimizeLoops=0;
			Cryptor aSeed = initialize(_randomizeFraction, _cipher);
			if (GlobalStore.getInstance().getVerbose())
				System.out.println((new Date()).toString() + " " + _name + " Reset with " + aSeed);
			optimize(aSeed, _cipher);
		}
		return _loops;
	}

	private void optimize(final Cryptor iCryptor, final Cipher iCipher) throws Exception {
		Cryptor aCurrentCryptor = new Cryptor(iCryptor);
		Score aBestScore=new Score(_letterSequences, aCurrentCryptor.decipher(iCipher));
		Move aBestMove=null;
		Score aPreviousScore;

		do {
			aPreviousScore=aBestScore;
			aBestScore=null;

			for (Character aSymbol : iCipher.getSymbols())
				for (Integer aTo = 0; aTo < aCurrentCryptor.getAlphabetSize(); aTo++) {
					Move aCurrentMove=new SymbolMove(aCurrentCryptor, aSymbol, aTo);
					Score aScore=aCurrentMove.checkMove(_cipher, _letterSequences);
					if (aScore!=null && (aBestScore==null || aScore.compareTo(aBestScore)>0)) {
						aBestScore=aScore;
						aBestMove=aCurrentMove;
					}
				}

			for (Integer aFrom=0; aFrom < aCurrentCryptor.getAlphabetSize(); aFrom++)
				for (Integer aTo=aFrom+1; aTo < aCurrentCryptor.getAlphabetSize(); aTo++) {
					Move aCurrentMove=new CharMove(aCurrentCryptor, aFrom, aTo);
					Score aScore=aCurrentMove.checkMove(_cipher, _letterSequences);
					if (aScore!=null && (aBestScore==null || aScore.compareTo(aBestScore)>0)) {
						aBestScore=aScore;
						aBestMove=aCurrentMove;
					}
				}

			if (aBestScore!=null && aBestScore.compareTo(aPreviousScore)>0) {
				aBestMove.apply();
				if (GlobalStore.getInstance().checkBest(aCurrentCryptor, aBestScore, _name, _loops))
					System.out.println(printBest(aCurrentCryptor, aBestScore) + " " + aCurrentCryptor.decipher(iCipher));
			}

			_loops._optimizeLoops++;
		} while ( aBestScore.compareTo(aPreviousScore)>0 );
	}

	private String printBest(final Cryptor iCryptor, final Score iScore) {
		final StringBuffer aStrBuf = new StringBuffer();
		aStrBuf.append((new Date()).toString() + " " + _name + " Iteration: " + this._loops + " " + iScore + " " + iCryptor.toString());

		return aStrBuf.toString();
	}

	private Cryptor initialize(final Double iFraction, final Cipher iCipher) {
		final Cryptor aCryptor;

		if (GlobalStore.getInstance().getBest().getScore() == null) {
			aCryptor = new Cryptor(Init.RANDOM, iCipher.getSymbols(), _alphabet);
		} else {
			aCryptor = new Cryptor(GlobalStore.getInstance().getBest().getCryptor());
			aCryptor.setInitMode(Init.COPIED);
			aCryptor.randomize(iFraction);
		}

		return aCryptor;
	}
}