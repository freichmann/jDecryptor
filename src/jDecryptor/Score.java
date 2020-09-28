/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2018
 */

package jDecryptor;

import java.text.DecimalFormat;
import java.util.Set;

public class Score implements Comparable<Score> {
	private final Double _patternScore;
	public final StringBuffer _log = new StringBuffer();

	@Override
	public int compareTo(Score iThat) {
		return this.rate().compareTo(iThat.rate());
	}

	public boolean equals(Score iThat) {
		return this._patternScore == iThat._patternScore && this._patternScore == iThat._patternScore;
	}

	public Score(final Set<LanguageStatistics> iNorms, final String iClear) throws Exception {
		Double aScore = 0.0;

		for (LanguageStatistics aNorm : iNorms) {
			// log(Poisson) score is actually not really normal distributed
			org.apache.commons.math3.distribution.NormalDistribution aNormalDist = new org.apache.commons.math3.distribution.NormalDistribution(aNorm.getScoreMean(iClear.length()),aNorm.getScoreSigma(iClear.length()));
			Double aLengthScore = aNorm.rate(iClear);

			Double aLnScore=aNormalDist.logDensity(aLengthScore);
			aScore += aLnScore;
			
			if (GlobalStore.getInstance().getVerbose()) {
				DecimalFormat aFormat = new DecimalFormat("0.00E0");
				if (_log.length()>0)
					_log.append(" ");
				_log.append(aNorm.getLength() + ":" + aFormat.format(aLnScore));
			}
		}
		_patternScore=aScore;
	}

	Score(Score iThat) {
		this._patternScore = iThat._patternScore;
	}

	public Double rate() {
		return _patternScore;
	}

	@Override
	public String toString() {
		StringBuffer aStrBuf = new StringBuffer();
		DecimalFormat aFormat = new DecimalFormat("0.000000000000000E0");

		aStrBuf.append("Score: " + (this.rate().isNaN() ? "NaN" : aFormat.format(this.rate())));

		if (GlobalStore.getInstance().getVerbose())
			aStrBuf.append(" " + dumpLog());

		return aStrBuf.toString();
	}

	public String dumpLog() {
		return "{"+_log.toString()+"}";
	}
}
