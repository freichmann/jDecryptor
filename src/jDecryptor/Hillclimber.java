/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2020
 */

package jDecryptor;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;

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
	private final Double _fuzzy;
	private Cryptor _climberBestCryptor=null;
	private Score _climberBestScore=null;

	public Hillclimber(final String iName, final Cipher iCipher, final Integer iMaximumLoops, final Set<LanguageStatistics> iLetterSequences, final Set<String> iAlphabet, final Double iRandomizeFraction, final Double iFuzzy) throws Exception {
		_letterSequences = iLetterSequences;
		_name = iName;
		_randomizeFraction = iRandomizeFraction;
		_cipher = iCipher;
		_maximumLoops = iMaximumLoops;
		_alphabet = iAlphabet;
		_fuzzy = iFuzzy;
	}

	@Override
	public LoopCounter call() throws Exception {
		while (true) {
			long aFail=0;
			do {
				_loops._resets++;
				_loops._optimizeLoops=0;
				Cryptor aSeed=initialize(_randomizeFraction, _cipher);
				if (!optimize(aSeed, _cipher))
					aFail++;
				else
					aFail=0;
			} while (_maximumLoops>aFail);

			_climberBestCryptor=null;
			_climberBestScore=null;
		}
	}

	private boolean optimize(final Cryptor iCryptor, final Cipher iCipher) throws Exception {
		boolean aEverFoundImprovement=false;
		Cryptor aCurrentCryptor = new Cryptor(iCryptor);
		Score aLoopBestScore=new Score(_letterSequences, aCurrentCryptor.decipher(iCipher));
		Double aCurrentTolerance=0.02;

		if (_climberBestScore==null || aLoopBestScore.compareTo(_climberBestScore)>0) {
			_climberBestScore=aLoopBestScore;

			if (GlobalStore.getInstance().checkBest(aCurrentCryptor, aLoopBestScore, _name, _loops))
				System.out.println(printGlobalBest(aCurrentCryptor, aLoopBestScore, aCurrentTolerance) + " " + aCurrentCryptor.decipher(iCipher));
		}

		Score aLastScore;
		boolean aLoopImproved;

		do {
			aLastScore=aLoopBestScore;
			Move aBestChoiceSoFar=null;
			long aTolerated=0;
			aLoopImproved=false;

			for (Character aSymbol : iCipher.getSymbols()) {
				for (Integer aTo = 0; aTo<aCurrentCryptor.getAlphabetSize(); aTo++) {
					Move aCurrentMove=new SymbolMove(aCurrentCryptor, aSymbol, aTo);
					Score aCurrentScore=aCurrentMove.checkMove(_cipher, _letterSequences);
					final Double aTolerance=aCurrentTolerance*GlobalStore._random.nextDouble();

					if (aCurrentScore!=null && (aCurrentScore.rate()*(1.0-aTolerance)>aLastScore.rate())) {
						if (aCurrentScore.compareTo(aLastScore)<0)
							aTolerated++;
						aLastScore=aCurrentScore;
						aBestChoiceSoFar=aCurrentMove;

						if (aCurrentScore.compareTo(aLoopBestScore)>0) {
							aLoopBestScore=aCurrentScore;
							aLoopImproved=true;
							if (aCurrentScore.compareTo(_climberBestScore)>0) {
								aEverFoundImprovement=true;
								_climberBestScore=aCurrentScore;
								_climberBestCryptor=new Cryptor(aCurrentCryptor);
								if (GlobalStore.getInstance().checkBest(aCurrentCryptor, aLoopBestScore, _name, _loops))
									System.out.println(printGlobalBest(aCurrentCryptor, aLoopBestScore, aCurrentTolerance) + " " + aCurrentCryptor.decipher(iCipher));
							}
						}
					}
				}

				if (aBestChoiceSoFar!=null)
					aBestChoiceSoFar.apply();
			}

			if (new Double(aTolerated)>_fuzzy*new Double(_cipher.length()))
				aCurrentTolerance*=0.95;
			else {
				aCurrentTolerance*=1.05;
				if (aCurrentTolerance>1.0)
					aCurrentTolerance=1.0;
			}

			_loops._optimizeLoops++;
		} while ( aLoopImproved );

		return aEverFoundImprovement;
	}

	private String printGlobalBest(final Cryptor iCryptor, final Score iScore, final Double iTolerance) {
		DecimalFormat aFormat = new DecimalFormat("0.0000");
		final StringBuffer aStrBuf = new StringBuffer();
		aStrBuf.append((new Date()).toString() + " " + _name + " Iteration: " + this._loops
				+ " " + iScore.toString()
				+ " Tolerance:" + aFormat.format(iTolerance)
				+ " Cryptor:" + iCryptor.toString());

		return aStrBuf.toString();
	}

	private Cryptor initialize(final Double iFraction, final Cipher iCipher) {
		final Cryptor aCryptor;

		if (_climberBestCryptor == null) {
			aCryptor = new Cryptor(Init.RANDOM, iCipher.getSymbols(), _alphabet);
		} else {
			aCryptor = new Cryptor(_climberBestCryptor);
			aCryptor.setInitMode(Init.COPIED);
			aCryptor.randomize(iFraction);
		}

		return aCryptor;
	}
}