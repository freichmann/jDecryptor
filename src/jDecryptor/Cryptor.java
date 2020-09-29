/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2020
 */

package jDecryptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum Init {
	RANDOM, COPIED
};

public class Cryptor {
	private final Vector<Character> _letters = new Vector<Character>();
	private final HashMap<Character, Integer> _symbols = new HashMap<Character, Integer>();
	private Init _random;

	public Cryptor(final Init iRandom, final Vector<Character> iLetters, final HashMap<Character, Integer> iSymbols) {
		_letters.addAll(iLetters);

		for (Character aSymbol : iSymbols.keySet())
			_symbols.put(aSymbol, iSymbols.get(aSymbol));
	}

	public Cryptor(final String iString) throws Exception {
		// NOROTATE,NOSWAP,COPIED,"[a-z]+",{[a:S6G8][b:V][c:e][d:fz][e:pEWZ+lN][f:QJ][g:R][h:)M][i:PU9k][j:][k:/][l:B#%][m:q][n:D(^O][o:!TdX][p:=][q:][r:rt\][s:@F7K][t:5HIL][u:Y][v:c][w:A][x:j][y:_][z:]}
// Mon Feb 17 09:06:59 CET 2020 Climber 1 Iterations: (1,5158) Score: -2,979068968024821E1 RANDOM,"[a-z]+",{[a:(+][b:@][c:^][d:c][e:BTEVW9jk<lM][f:1][g:;/][h:3tf][i:pH|][j:][k:][l:&)L][m:G][n:R7*K][o:268X:-][p:>][q:][r:C4DU_][s:Pb#SdFYZ][t:5yJz.NO][u:%][v:][w:A][x:][y:q][z:]} ienpieceseiflemostiaeasturestennhletodearsmeallstitsiinceonehitaannoreeathesitssalregioncsetonsresporastanyugordomettliamothestartstableseeasasnosedtwornotereactatinesetorangtheirseiseentoedentinteretalestoestsatitsedagonemstchtorstderhaateofnorereperstoaidthtenatictheymondealforeaseeeillaalertedstsiegaliiseseenherstenordperitieststowinga
		String aPatternString = "(" + Init.COPIED + "|" + Init.RANDOM + ")," + "(\\{.*\\})";

		Pattern aPattern = Pattern.compile(aPatternString);
		Matcher aMatcher = aPattern.matcher(iString);

		if (aMatcher.find()) {
			this._random = Init.valueOf(aMatcher.group(1));
			String aCryptor = aMatcher.group(2);

			System.out.println("Parsing cryptor from " + aCryptor);
			Pattern aCryptorPattern = Pattern.compile("(\\[?(\\w):(\\S*?)\\][\\[\\}])");
			Matcher aCryptorMatcher = aCryptorPattern.matcher(aCryptor);
			
			Integer aInt = 0;
			while (aCryptorMatcher.find()) {
				this._letters.add(aCryptorMatcher.group(2).charAt(0));

				for (Character aChar : aCryptorMatcher.group(3).toCharArray()) {
					this._symbols.put(aChar, aInt);
				}
				aInt++;
			}
		} else
			throw new Exception("Failed to parse " + iString);
	}

	public Cryptor(final Init iRandom, final Set<Character> iSymbols, Set<String> iAlphabet) {
		_random = iRandom;

		Vector<String> aVector = new Vector<String>();

		aVector.addAll(iAlphabet);

		while (aVector.size() > 0)
			_letters.add(aVector.remove(0).charAt(0));

		{
			int aPos = 0;
			
			for (Character aChar : iSymbols) {
				if (_random == Init.RANDOM)
					_symbols.put(aChar, GlobalStore._random.nextInt(_letters.size()));
				else {
					_symbols.put(aChar, aPos % _letters.size());
					aPos++;
				}
			}
		}
	}

	public Cryptor(Cryptor iThat) {
		this._letters.addAll(iThat._letters);
		this._symbols.putAll(iThat._symbols);
		this._random = iThat._random;
	}

	public boolean equals(Cryptor iThat) {
		return this._letters.equals(iThat._letters) && this._symbols.equals(iThat._symbols);
	}

	public void randomize(final Double iFraction) {
		Vector<Character> aSymbolVector = new Vector<Character>();
		aSymbolVector.addAll(_symbols.keySet());

		for (int aInt = 0; aInt < aSymbolVector.size()*iFraction; aInt++) {
			Character aSymbol = aSymbolVector
					.elementAt(GlobalStore._random.nextInt(aSymbolVector.size()));
			Integer aTo = GlobalStore._random.nextInt(_letters.size());
			moveSymbol(aSymbol, aTo);
		}
	}

	public void setInitMode(final Init iInit) {
		_random = iInit;
	}

	public Integer getAlphabetSize() {
		return _letters.size();
	}

	public String decipher(final Cipher iCipher) throws Exception {
		StringBuffer aStringBuffer = new StringBuffer();

		for (int i = 0; i < iCipher.length(); i++)
			aStringBuffer.append(this.getClear(iCipher.getAt(i), i));
		return aStringBuffer.toString();
	}

	public String encrypt(final String iMessage) {
		StringBuffer aStringBuffer = new StringBuffer();

		for (int aOffset = 0; aOffset < iMessage.length(); aOffset++) {
			Integer aPos = _letters.indexOf(iMessage.charAt(aOffset));

			if (_symbols.containsValue(aPos)) {
				Vector<Character> aVec = symbolsAtPos(aPos);
				aStringBuffer.append(aVec.get(GlobalStore._random.nextInt(aVec.size())));
			}
		}

		return aStringBuffer.toString();
	}

	private Vector<Character> symbolsAtPos(Integer aPos) {
		HashSet<Character> aSet = new HashSet<Character>();

		for (Character aChar : _symbols.keySet())
			if (_symbols.get(aChar).equals(aPos))
				aSet.add(aChar);

		Vector<Character> aVec = new Vector<Character>();
		aVec.addAll(aSet);
		return aVec;
	}

	private Character getClear(Character iSymbol, int iOffset) throws Exception {
		return _letters.elementAt(_symbols.get(iSymbol));
	}

	public Integer moveSymbol(Character iSymbol, Integer iTo) {
		Integer aFrom = _symbols.get(iSymbol);
		if (aFrom != null && aFrom != iTo) {
			_symbols.put(iSymbol, iTo);
			return aFrom;
		}
		return null;
	}
	
	public void swapChar(Integer iFrom, Integer iTo) {
		Character aChar=_letters.elementAt(iTo);
		_letters.set(iTo, _letters.elementAt(iFrom));
		_letters.set(iFrom, aChar);		
	}

	public String toString() {
		StringBuffer aStrBuf = new StringBuffer( _random + ",{");

		for (int aPos = 0; aPos < _letters.size(); aPos++) {
			aStrBuf.append("[" + _letters.elementAt(aPos) + ":");
			Vector<Character> aVec = symbolsAtPos(aPos);
			for (Character aChar : aVec)
				aStrBuf.append(aChar);
			aStrBuf.append("]");
		}
		return aStrBuf.append("}").toString();
	}
}