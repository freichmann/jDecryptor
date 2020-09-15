package jDecryptor.Move;

import jDecryptor.Cryptor;

public class SymbolMove extends Move {
	final Character _symbol;
	final Integer _to;
	Integer _from=null;
	
	public SymbolMove(Cryptor iCryptor, Character iSymbol, Integer iTo) {
		super(iCryptor);
		_symbol=iSymbol;
		_to=iTo;
	};
	
	public boolean apply() {
		_from=_cryptor.moveSymbol(_symbol, _to);
		return (_from!=null);
	}

	public boolean fallback() {
		if (_from!=null) {
			_cryptor.moveSymbol(_symbol, _from);
			_from=null;
			return true;
		}
		return false;
	}
}
