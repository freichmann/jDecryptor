/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2018
 */

package jDecryptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;

public class LanguageStatistics implements Callable<LanguageStatistics> {

	// Language Statistic global
	private final Integer _length;
	private Long _samples=0L;
	private final Map<Integer,Double> _scoreMeans=new HashMap<Integer,Double>();
	private final Map<Integer,Double> _scoreSigmas=new HashMap<Integer,Double>();
	private String _hugeText=null;
	private Integer _cipherLength=0;

	final private Map<String, Long> _sequenceStats = new HashMap<String,Long>();

	@Override // builds language statistics score statistics from corpus
	public LanguageStatistics call() throws Exception {
		Vector<Double> aValues = new Vector<Double>();
		
		for (int aIndex = 0; aIndex <= _hugeText.length() - _cipherLength; aIndex+=_cipherLength)
			aValues.add(rate(_hugeText.substring(aIndex, aIndex + _cipherLength)));

		Double aMean=0.0;
		for (Double aValue:aValues)
			aMean+=aValue;
		aMean/=aValues.size();

		Double aVariance=0.0;
		org.apache.commons.math3.analysis.function.Pow aPow=new org.apache.commons.math3.analysis.function.Pow();
		org.apache.commons.math3.analysis.function.Sqrt aSqrt=new org.apache.commons.math3.analysis.function.Sqrt();
		for (Double aValue:aValues)
			aVariance+=aPow.value(aValue-aMean,2.0);
		Double aSigma = aSqrt.value(aVariance/(aValues.size()-1));

		this._scoreMeans.put(_cipherLength, aMean);
		this._scoreSigmas.put(_cipherLength, aSigma);
		return this;
	}

	public LanguageStatistics(final Integer iLength) {
		_length = iLength;
	}

	public Set<String> getSequences() {
		return _sequenceStats.keySet();
	}

	public Long getSamples() {
		return _samples;
	}

	public Integer getLength() {
		return _length;
	}

	public Long getCount(final String iString) {
		Long aCount = _sequenceStats.get(iString);
		return (aCount != null ? aCount : 0);
	}

	public Double empiricalMean(final String iString) {
		return new Double(getCount(iString)) / getSamples();
	}

	public void addStringAsSequences(String iString) {
		for (int aIndex = 0; aIndex <= iString.length() - this._length; aIndex++) {
			String aString = iString.substring(aIndex, aIndex + this._length);
			Long aCount = _sequenceStats.get(aString);
			if (aCount == null)
				_sequenceStats.put(aString, 1L);
			else
				_sequenceStats.put(aString, aCount+1L);
			_samples++;
			if (_samples<0)
				throw new Error("Long overrun");
		}
	}

	public boolean addSequence(String iString, Long iCount) {
		if (_length==iString.length()) {
			Long aCount = _sequenceStats.get(iString);
			if (aCount==null)
				aCount=0L;
			_sequenceStats.put(iString, aCount + iCount);
			_samples+=iCount;
			if (_samples<0)
				throw new Error("Long overrun");
			return true;
		} else
			return false;
	}

	public Double getScoreMean(Integer iLength) {
		return _scoreMeans.get(iLength);
	}

	public Double getScoreSigma(Integer iLength) {
		return _scoreSigmas.get(iLength);
	}

	public void setTextCipherLength(String iHugeText, Integer iCipherLength) {
		_hugeText=iHugeText;
		_cipherLength=iCipherLength;		
	}

	public Double rate(final String iClear) throws Exception {
		Double aScore = 0.0;

		org.apache.commons.math3.analysis.function.Log aLog=new org.apache.commons.math3.analysis.function.Log();
		final Double aNeverSeen=aLog.value(2.0)/this.getSamples().doubleValue();
		LanguageStatistics aObservedStatistics = new LanguageStatistics(this.getLength());
		aObservedStatistics.addStringAsSequences(iClear);

		//				Integer aN=iClear.length()-aLength+1;
		for (String aString:aObservedStatistics.getSequences()) {
			Double aP=this.empiricalMean(aString);
			if (aP==0.0)
				aP=aNeverSeen;
			Long aK=aObservedStatistics.getCount(aString);

			//					Gauss
			//					aLengthScore += aK*(2*aN*aP-aK)/(aN*aP*(1-aP)); // Only good for npq>9

			//					Binomial
			//					org.apache.commons.math3.distribution.BinomialDistribution aBinDist = new org.apache.commons.math3.distribution.BinomialDistribution(aN,aP);
			//					aLengthScore += aBinDist.logProbability(aK)-aBinDist.logProbability(0);

			//					Poisson
			aScore -= org.apache.commons.math3.util.CombinatoricsUtils.factorialLog(aK.intValue()) - aK*aLog.value(aP);
		}					

		return aScore;
	}

	public String dump() {
		StringBuffer aStrBuf=new StringBuffer();
		boolean aFirst=true;
		for (String aString:this.getSequences()) {
			if (!aFirst)
				aStrBuf.append("\n");
			else
				aFirst=false;

			aStrBuf.append(aString).append(" ").append(this.getCount(aString));
		}
		return aStrBuf.toString();
	}

	@Override
	public String toString() {
		return _length + "-grams " + "Samples: " + getSamples();
	}
}