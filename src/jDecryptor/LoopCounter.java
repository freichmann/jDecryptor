/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2020
 */

package jDecryptor;

public class LoopCounter {
	public Integer _optimizeLoops = 0;
	public Integer _resets = 0;
	public Integer _tolerance = 0;

	@Override
	public String toString() {
		return "(" + _resets + "," + _optimizeLoops + ")";
	}
}
