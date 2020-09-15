/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2018
 */

package jDecryptor;

public class RatedCryptor {
	private final Cryptor _cryptor;
	private final Score _score;

	public RatedCryptor(Cryptor iCryptor, Score iScore) {
		_cryptor = iCryptor;
		_score = iScore;
	}

	public RatedCryptor(RatedCryptor iThat) {
		_cryptor = new Cryptor(iThat.getCryptor());
		_score = new Score(iThat.getScore());
	}

	public Cryptor getCryptor() {
		return _cryptor;
	}

	public Score getScore() {
		return _score;
	}
}