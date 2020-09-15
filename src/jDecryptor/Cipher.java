/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2018
 */

package jDecryptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

public class Cipher {
	final private Integer _length;
	final private Character[] _cipher;

	public Cipher(Cipher iCipher) {
		this._length = iCipher._length;
		this._cipher = new Character[this._length];

		for (int i = 0; i < this._length; i++)
			this._cipher[i] = iCipher._cipher[i];

	}

	public Cipher(final String iCipher) {
		this._length = iCipher.length();
		this._cipher = new Character[this._length];

		for (Integer aInt = 0; aInt < this._length; aInt++)
			this._cipher[aInt] = iCipher.charAt(aInt);
	}

	public Integer length() {
		return _length;
	}

	public Map<String, Set<Integer>> kasiski(final Integer aLength) {
		Map<String, Set<Integer>> aRepeats = new HashMap<String, Set<Integer>>();

		for (Integer aSearchPos = 0; aSearchPos <= _cipher.length - aLength; aSearchPos++) {
			StringBuffer aStrBuf = new StringBuffer();

			for (Integer aInt = 0; aInt < aLength; aInt++)
				aStrBuf.append(_cipher[aSearchPos + aInt]);

			if (aRepeats.containsKey(aStrBuf.toString()))
				continue;

			final Set<Integer> aSet = new HashSet<Integer>();
			for (Integer aStartPos = aSearchPos + aLength; aStartPos <= _cipher.length - aLength; aStartPos++) {
				Integer aRunningPos = 0;
				boolean aMatches = true;

				do
					aMatches &= (_cipher[aSearchPos + aRunningPos] == _cipher[aStartPos + aRunningPos]);
				while (aMatches && ++aRunningPos < aLength);

				if (aMatches)
					aSet.add(aStartPos - aSearchPos);
			}

			if (!aSet.isEmpty())
				aRepeats.put(aStrBuf.toString(), aSet);
		}
		return aRepeats;
	}

	public Vector<String> split(final Integer iPeriod) {
		if (iPeriod < 1)
			return null;

		Vector<String> aStringMap = new Vector<String>();

		for (Integer aOffset = 0; aOffset < iPeriod; aOffset++) {
			StringBuffer aStrBuf = new StringBuffer();
			for (Integer aPos = aOffset; aPos < this.length(); aPos += iPeriod)
				aStrBuf.append(_cipher[aPos]);
			aStringMap.add(aStrBuf.toString());
		}
		return aStringMap;
	}

	public static String unsplit(final Vector<String> iMap) {
		StringBuffer aStrBuf = new StringBuffer();

		Integer aMaxLength = 0;
		for (String aString : iMap)
			if (aString.length() > aMaxLength)
				aMaxLength = aString.length();

		for (Integer aInt = 0; aInt < aMaxLength; aInt++)
			for (String aString : iMap)
				if (aInt < aString.length())
					aStrBuf.append(aString.charAt(aInt));

		return aStrBuf.toString();
	}

	public void reverse(Integer iLength) throws Exception {
		Integer aPos = 0;
		while (aPos < _cipher.length) {
			Integer aEnd = Math.min(iLength, _cipher.length - aPos);
			for (Integer aInt = 0; aInt < aEnd / 2; aInt++)
				swapSymbols(aPos + aInt, aPos + aEnd - 1 - aInt);
			aPos += iLength;
		}
	}

	private void swapSymbols(Integer iFrom, Integer iTo) throws Exception {
		if (iFrom != iTo)
			if (iFrom < this.length() && iTo < this.length()) {
				final Character aTmp = _cipher[iFrom];
				_cipher[iFrom] = _cipher[iTo];
				_cipher[iTo] = aTmp;
			} else
				throw new Exception("Out of bounds");
	}

	public char getAt(int iPosition) {
		return _cipher[iPosition];
	}

	public TreeSet<Character> getSymbols() {
		TreeSet<Character> aSet = new TreeSet<Character>();

		for (int aInt = 0; aInt < _cipher.length; aInt++)
			aSet.add(_cipher[aInt]);

		return aSet;
	}

	public String toString() {
		StringBuffer aStringBuffer = new StringBuffer();
		Map<Character,Integer> aMap=new HashMap<Character,Integer>();
		Integer aCounter=1;
		boolean aFirst=true;

		for (int aInt = 0; aInt < _length; aInt++) {
			if (!aMap.containsKey(_cipher[aInt]))
				aMap.put(_cipher[aInt], aCounter++);
			if (!aFirst)
				aStringBuffer.append(",");
			aStringBuffer.append(aMap.get(_cipher[aInt]));
			aFirst=false;
		}

		return aStringBuffer.toString();
	}
}
