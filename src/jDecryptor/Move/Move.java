package jDecryptor.Move;

import java.util.Set;

import jDecryptor.Cipher;
import jDecryptor.Cryptor;
import jDecryptor.GlobalStore;
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
			final Long aHash = this.hashCode(aDeciphered);

			if (!GlobalStore.getInstance().isCovered(aHash)) {
				GlobalStore.getInstance().addCovered(aHash);

				aScore = new Score(iLetterSequences, aDeciphered);
			}
			this.fallback();
		}
		return aScore;
	}
	
	private Long hashCode(String iString) {
		Long h=0L;
		{
			int off = 0;
			byte val[] = iString.getBytes();
			int len = iString.length();

			for (int i = 0; i < len; i++) {
				h = 31*h + val[off++];
			}
		}
		return h;
	}
}
