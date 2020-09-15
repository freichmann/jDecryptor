package jLanguage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jDecryptor.LanguageStatistics;

public class Language {

	public static void main(String[] iArgs) {
		CommandLineParser aCommandParser = new DefaultParser();
		HelpFormatter aFormatter = new HelpFormatter();
		CommandLine aCommandLine;
		Options aOptions = new Options();
		final Set<LanguageStatistics> aLetters = new HashSet<LanguageStatistics>();
		
		System.err.println("Version 25.2.2020 18:02");

		{
			Option aOpt = new Option("l", "language", true, "language sample files");
			aOpt.setRequired(true);
			aOptions.addOption(aOpt);
		}
		
		{
			Option aOpt = new Option("x", "exclude", true, "exclude filter when reading language corpus");
			aOpt.setRequired(false);
			aOptions.addOption(aOpt);
		}

		{
			Option aOpt = new Option("n", "ngrams", true, "length");
			aOpt.setRequired(true);
			aOptions.addOption(aOpt);
		}

		try {
			aCommandLine = aCommandParser.parse(aOptions, iArgs);
		} catch (ParseException aException) {
			System.err.println("Exception: " + aException.getMessage());
			aFormatter.printHelp("jDecryptor", aOptions);

			System.exit(1);
			return;
		}

		String aLangs[] = aCommandLine.getOptionValues("language");
		final String aExcludeFilter = aCommandLine.getOptionValue("exclude", "[^a-z]+");
		String aNgrams[] = aCommandLine.getOptionValues("ngrams");

		// TODO Auto-generated method stub
		final StringBuffer aHugeText = new StringBuffer();
		for (String aLangFile : aLangs) {
			System.err.println("Reading " + aLangFile);
			BufferedInputStream aIn;
			try {
				aIn = new BufferedInputStream(Files.newInputStream(Paths.get(aLangFile)));
				Scanner aScanner = new Scanner(aIn);
				while (aScanner.hasNextLine())
					aHugeText.append(aScanner.nextLine().toLowerCase().replaceAll(aExcludeFilter, ""));
				aScanner.close();

				aIn.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		if (aHugeText.length() > 0) {
			for (String aString : aNgrams) {
				Integer aLength=Integer.valueOf(aString);
				LanguageStatistics aLangStat = new LanguageStatistics(aLength);
				System.err.print("Counting each of "
						+ aLength + "-grams: ");
				aLangStat.addStringAsSequences(aHugeText.toString());
				System.err.println(aLangStat.getSequences().size() + " distinct found.");
				aLetters.add(aLangStat);
			}
		} else {
			System.err.println("No sample texts available to run statistic comparison with");
			System.exit(-1);
		}

		for (LanguageStatistics aStats : aLetters) {
			System.out.println(aStats.dump());
		}
	}
}
