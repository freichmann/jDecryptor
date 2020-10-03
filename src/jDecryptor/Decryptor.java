/**
 * Licensed under GPLv3
 * 
 * Fritz Reichmann, 2016-2020
 */

package jDecryptor;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

enum ReadMode {
	BIN, TXT, NUM
};

public class Decryptor {
	public static void main(String[] iArgs) {
		try {
			System.out.println("Version 3.10.2020 19:38");

			Options aOptions = new Options();
			{
				Option aOpt = new Option("a", "verbose", false, "verbose output");
				aOpt.setRequired(false);
				aOptions.addOption(aOpt);
			}

			{
				Option aOpt = new Option("c", "cipher", true, "cipher file");
				aOpt.setRequired(true);
				aOptions.addOption(aOpt);
			}

			{
				Option aOpt = new Option("d", "debug", false, "debug mode");
				aOpt.setRequired(false);
				aOptions.addOption(aOpt);
			}

			{
				Option aOpt = new Option("i", "iterations", true, "maximum resets per thread");
				aOpt.setRequired(false);
				aOptions.addOption(aOpt);
			}

			{
				Option aOpt = new Option("l", "language", true, "language n-gram files");
				aOpt.setRequired(true);
				aOptions.addOption(aOpt);
			}

			{
				Option aOpt = new Option("m", "mode", true, "cipher file read mode");
				aOpt.setRequired(true);
				aOptions.addOption(aOpt);
			}

			{
				Option aOpt = new Option("r", "scramble", true, "how often and much to randomize when starting a new iteration");
				aOpt.setRequired(false);
				aOptions.addOption(aOpt);
			}

			{
				Option aOpt = new Option("s", "seed", true, "key to start with");
				aOpt.setRequired(false);
				aOptions.addOption(aOpt);
			}
			
			{
				Option aOpt = new Option("t", "threads", true, "number of parallel threads");
				aOpt.setRequired(false);
				aOptions.addOption(aOpt);
			}

			{
				Option aOpt = new Option("w", "huge", true, "text sample to build score statistics from");
				aOpt.setRequired(true);
				aOptions.addOption(aOpt);
			}
			
			CommandLineParser aCommandParser = new DefaultParser();
			HelpFormatter aFormatter = new HelpFormatter();
			CommandLine aCommandLine;

			try {
				aCommandLine = aCommandParser.parse(aOptions, iArgs);
			} catch (ParseException aException) {
				System.err.println("Exception: " + aException.getMessage());
				aFormatter.printHelp("jDecryptor", aOptions);

				System.exit(1);
				return;
			}

			final Boolean aDebugFlag;
			if (aCommandLine.hasOption("debug"))
				aDebugFlag = true;
			else
				aDebugFlag = false;

			final Integer aMaxThreads;
			if (aCommandLine.hasOption("threads"))
				aMaxThreads = Integer.valueOf(aCommandLine.getOptionValue("threads"));
			else
				aMaxThreads = Runtime.getRuntime().availableProcessors();

			String aCipherFilename = aCommandLine.getOptionValue("cipher");
			String aLangs[] = aCommandLine.getOptionValues("language");
			String aHuges[] = aCommandLine.getOptionValues("huge");
			final Integer aMaximumLoops = Integer.valueOf(aCommandLine.getOptionValue("iterations", "0"));
			final ReadMode aReadMode = ReadMode.valueOf(aCommandLine.getOptionValue("mode"));
			final Double aRandomFraction = Double.valueOf(aCommandLine.getOptionValue("scramble", "1.0"));
			final String aSeed = aCommandLine.getOptionValue("seed", null);
			final boolean aVerbose = aCommandLine.hasOption("verbose");
			final Set<String> aAlphabet = new TreeSet<String>();
			GlobalStore.getInstance().setVerbose(aVerbose);

			System.out.println("Parallel threads: " + aMaxThreads);
			System.out.println("Maximum iterations per thread: " + (aMaximumLoops == 0 ? "unlimited" : aMaximumLoops));

			final Set<LanguageStatistics> aLetters = new HashSet<LanguageStatistics>();

			Pattern aPattern = Pattern.compile("^\\s*(\\w+)\\s+(\\d+)\\s*$");

			for (String aLangFile : aLangs) {
				System.out.println("Reading " + aLangFile);
				BufferedInputStream aIn = new BufferedInputStream(Files.newInputStream(Paths.get(aLangFile)));
				Scanner aScanner = new Scanner(aIn);

				while (aScanner.hasNextLine()) {
					String aLine = aScanner.nextLine();
					Matcher aMatcher = aPattern.matcher(aLine);
					if (aMatcher.find()) {
						String aNgram=aMatcher.group(1).toLowerCase();
						Long aCount=Long.valueOf(aMatcher.group(2));

						boolean aWasAdded=false;
						for (LanguageStatistics aLangStat:aLetters) {
							aWasAdded=aLangStat.addSequence(aNgram, aCount);
							if (aWasAdded)
								break;
						}
						if (!aWasAdded) {
							LanguageStatistics aLangStat=new LanguageStatistics(aNgram.length());
							aLangStat.addSequence(aNgram, aCount);
							aLetters.add(aLangStat);
						}

						for (Integer aInt=0; aInt<aNgram.length(); aInt++)
							aAlphabet.add(aNgram.substring(aInt, aInt+1));					
					}
					else
						System.err.println("Did not match: " + aLine);

				}
				aScanner.close();
				aIn.close();
			}

			for (LanguageStatistics aLangStat:aLetters) {
				System.out.println(aLangStat.toString());
			}

			final Cipher aCipher;
			String aDecrypted = null;
			ExecutorService aExecService = Executors.newFixedThreadPool(aMaxThreads);
			List<Future<LanguageStatistics>> aLangStatFutures = new ArrayList<Future<LanguageStatistics>>();

			{
				final StringBuffer aHugeText = new StringBuffer();
				for (String aHuge:aHuges) {
					System.out.println("Reading sample file " + Paths.get(aHuge));
					BufferedInputStream aIn = new BufferedInputStream(Files.newInputStream(Paths.get(aHuge)));
					Scanner aScanner = new Scanner(aIn);

					while (aScanner.hasNextLine())
						aHugeText.append(aScanner.nextLine());
					aScanner.close();
					aIn.close();			
				}

				System.out.println("Collected " + aHugeText.length() + " characters of text sample.");

				final String aCipherString = readCipher(aCipherFilename, aReadMode);
				if (aDebugFlag) {
					System.out.println("Debug mode output start");
					Set<Character> aSymbols = new HashSet<Character>();
					String aSymbolString = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+";
					for (int aPos = 0; aPos < aSymbolString.length(); aPos++)
						aSymbols.add(aSymbolString.charAt(aPos));
					String aMsg = "ilikekillingpeoplebecauseitissomuchfunitismorefunthankillingwildgameintheforestbecausemanisthemostdangerousanimalofalltokillsomethinggivesmethemostthrillingexperienceitisevenbetterthangettingyourrocksoffwithagirlthebestpartofitisthatwhenidieiwillbereborninparadiceandalltheihavekilledwillbecomemyslavesiwillnotgiveyoumynamebecauseyouwilltrytoslowdownorstopmycollectingofslavesformyafterlifeebeorietemethhpiti";
					Cryptor aEncryptor = new Cryptor(Init.COPIED, aSymbols, aAlphabet);
					Integer aLength = aMsg.length();
					System.out.println("Solution: " + aEncryptor);
					String aMapped = aMsg.toLowerCase();
					String aTruncated = aMapped.substring(0, Math.min(aLength, aMapped.length()));
					String aEncrypted = aEncryptor.encrypt(aTruncated);
					aCipher = new Cipher(aEncrypted);
					System.out.println("Encrypted: " + aEncrypted);
					aDecrypted = aEncryptor.decipher(aCipher);
				} else {
					System.out.println("Cipher from file : " + aCipherString);
					System.out.println("Cipher length : " + aCipherString.length());
					aCipher = new Cipher(aCipherString);
				}

				if (aHugeText.length() > 0) {
					System.out.println("Read " + aHugeText.length() + " of text sample.");
					DecimalFormat aFormat = new DecimalFormat("0.00E0");
					for (LanguageStatistics aLangStat : aLetters) {
						aLangStat.setTextCipherLength(aHugeText.toString(), aCipher.length());

						Future<LanguageStatistics> aFuture = aExecService.submit(aLangStat);
						aLangStatFutures.add(aFuture);
					}

					Double aLnPerfectSum=0.0;
					org.apache.commons.math3.analysis.function.Log aLn = new org.apache.commons.math3.analysis.function.Log();
					System.out.println("Poisson likelihood statistics from sample text");
					for (Future<LanguageStatistics> aLangStatFuture:aLangStatFutures) {
						Double aLnPerfect=-aLn.value(Math.sqrt(2*Math.PI)*aLangStatFuture.get().getScoreSigma(aCipher.length()));
						System.out.println(aLangStatFuture.get().getLength()+"-grams"
								+ " Mean:"+aFormat.format(aLangStatFuture.get().getScoreMean(aCipher.length()))
								+ " Sigma:"+aFormat.format(aLangStatFuture.get().getScoreSigma(aCipher.length()))
								+ " Perfect:"+aFormat.format(aLnPerfect));
						aLnPerfectSum+=aLnPerfect;
					}
					System.out.println("Perfect score: " + aFormat.format( aLnPerfectSum ));
				} else {
					System.err.println("No text sample to build statistics from. Stopping.");
					System.exit(1);
				}
			}

			if (aSeed != null) {
				String aLastLine = null;
				System.out.println("Reading seed key from file " + aSeed);
				Scanner aScanner = new Scanner(new FileReader(aSeed));
				aScanner.useDelimiter(Pattern.compile("\n"));

				while (aScanner.hasNext())
					aLastLine = aScanner.next();
				aScanner.close();

				Cryptor aDisk = new Cryptor(aLastLine);
				System.out.println("Extracted seed key: " + aDisk);
				String aDeciphered = aDisk.decipher(aCipher);
				System.out.println("Seed decipher: " + aDeciphered);
				Score aScore = new Score(aLetters, aDeciphered);
				System.out.println("Seed score: " + aScore);
				GlobalStore.getInstance().checkBest(aDisk, aScore, "seed", new LoopCounter());
			}

			System.out.println("Cipher to operate: " + aCipher);
			System.out.println("Cipher symbols count: " + aCipher.getSymbols().size());

			System.out.println("Cipher symbols : " + aCipher.getSymbols());
			System.out.println("Random fraction: " + aRandomFraction);
			System.out.println("Language alphabet: " + aAlphabet);
			System.out.println("Language alphabet Size: " + aAlphabet.size());

			if (aDecrypted != null) {
				Score aScore = new Score(aLetters, aDecrypted);
				System.out.println("Decrypted: " + aScore + " : " + aDecrypted);
				System.out.println("Test mode output end");
			}

			List<Future<LoopCounter>> aLoopCountFutures = new ArrayList<Future<LoopCounter>>();

			for (Integer aInt = 0; aInt < aMaxThreads; aInt++) {
				Future<LoopCounter> aFuture = aExecService.submit(
						new Hillclimber("Thread " + aInt, aCipher, aMaximumLoops, aLetters, aAlphabet, aRandomFraction));
				aLoopCountFutures.add(aFuture);
			}

			System.out.println((new Date()).toString() + ": Executor initialization completed.");
			aExecService.shutdown();

			for (Future<LoopCounter> aFuture : aLoopCountFutures)
				aFuture.get();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			System.err.println(e.getStackTrace());
			System.exit(1);
		}
	}

	private static String readCipher(final String iFilename, final ReadMode iReadMode)
			throws Exception {
		System.out.println("Reading cipher from file " + iFilename);
		StringBuffer aStrBuf = new StringBuffer();

		if (iReadMode == ReadMode.TXT)
			try (BufferedReader aBufferedReader = new BufferedReader(new FileReader(iFilename))) {
				String aCurrentLine;

				while ((aCurrentLine = aBufferedReader.readLine()) != null)
					aStrBuf.append(aCurrentLine.replaceAll("\n", "").replaceAll("\r", ""));
			}
		else if (iReadMode == ReadMode.BIN) {
			InputStream aInputStream = new FileInputStream(iFilename);
			int aByteRead;
			while ((aByteRead = aInputStream.read()) != -1)
				aStrBuf.append((char) (aByteRead));
			aInputStream.close();
		} else if (iReadMode == ReadMode.NUM) {
			Scanner aScanner = new Scanner(new FileReader(iFilename));
			aScanner.useDelimiter("\\s*(\\s|,)\\s*");
			while (aScanner.hasNextInt()) {
				aStrBuf.append((char) aScanner.nextInt());
			}
			aScanner.close();
		} else
			throw new Exception("Unknown read mode");

		return aStrBuf.toString();
	}
}