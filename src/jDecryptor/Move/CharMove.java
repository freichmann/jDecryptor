package jDecryptor.Move;

import jDecryptor.Cryptor;

public class CharMove extends Move {
	final Integer _from, _to;
	private boolean _applied=false;

	public CharMove(Cryptor iCryptor, Integer iFrom, Integer iTo) {
		super(iCryptor);
		_from=iFrom;
		_to=iTo;
	}

	@Override
	public boolean apply() {
		if (!_applied) {
			_cryptor.swapChar(_from, _to);		
			_applied=true;
			return true;
		} else
			return false;
	}

	@Override
	public boolean fallback() {
		if (_applied) {
			_cryptor.swapChar(_from, _to);
			_applied=false;
			return true;
		} else
			return false;
	};




}
