package jDecryptor.Move;

import java.util.Set;

import jDecryptor.Cipher;
import jDecryptor.Cryptor;
import jDecryptor.LanguageStatistics;
import jDecryptor.Score;

public abstract class Move {
	final Cryptor _cryptor;

	public abstract boolean apply();
	public abstract boolean fallback();

	public Move(Cryptor iCryptor) {
		_cryptor=iCryptor;
	};

	public Score checkMove(final Cipher iCipher, Set<LanguageStatistics> iLetterSequences)
			throws Exception {
		Score aScore=null;
		if (this.apply()) {
			final String aDeciphered = _cryptor.decipher(iCipher);

			aScore = new Score(iLetterSequences, aDeciphered);
			this.fallback();
		}
		return aScore;
	}
}