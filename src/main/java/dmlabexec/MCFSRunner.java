package dmlabexec;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVWriter;
import dmLab.attrSelection.MCFSParams;
import dmLab.attrSelection.attrImportance.AttributesImportance;
import dmLab.attrSelection.attrImportance.Ranking;
import dmLab.attrSelection.mcfs.framework.MCFSClassicThreads;
import dmLab.attrSelection.mcfs.framework.MCFSFrameworkThreads;
import dmLab.container.array.Array;

public class MCFSRunner {

  public static void main(String argv[]) throws Exception {
    CommandLineParser parser = new PosixParser();
    CommandLine line = null;
    Options options = createOptions();
    try {
      line = parser.parse(options, argv);
    } catch (ParseException exp) {
      System.err.println("Invalid arguments " + exp.getMessage());
      printUsage(options);
      return;
    }

    int start = 1;
    if (line.hasOption("start")) {
      start = ((Number) line.getParsedOptionValue("start")).intValue();
    }
    int end = 100;
    if (line.hasOption("end")) {
      end = ((Number) line.getParsedOptionValue("end")).intValue();
    }

    long initialSeed = System.currentTimeMillis();
    if (line.hasOption("seed")) {
      initialSeed = ((Number) line.getParsedOptionValue("seed")).longValue();
    }

    int projections = 4000;
    if (line.hasOption("projections")) {
      projections = ((Number) line.getParsedOptionValue("projections")).intValue();
    }

    System.out.println("Running:\n\tstart:\t" + start + "\n\tend:\t" + end + "\n\tseed:\t" + initialSeed
        + "\n\tprojections:\t" + projections);
    Thread.sleep(2000);

    Generator generator = new Generator();
    SeedAccepter accepter = new DuplicateCheck();

    generator.setInitialSeed(initialSeed);

    NumberFormat expNameFormat = new DecimalFormat(StringUtils.repeat("0", String.valueOf(end).length()));

    CSVWriter all = new CSVWriter(new FileWriter("exp_" + initialSeed + "_all.csv"), CSVWriter.DEFAULT_SEPARATOR,
        CSVWriter.NO_QUOTE_CHARACTER);
    CSVWriter significant = new CSVWriter(new FileWriter("exp_" + initialSeed + "_sig.csv"),
        CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);

    File tmpRoot = new File("tmp");
    if (!tmpRoot.exists()) {
      tmpRoot.mkdir();
    }
    File tmp = new File(tmpRoot, String.valueOf(initialSeed));
    tmp.mkdir();
    try {
      for (int i = start; i <= end; i++) {
        System.out.println("experiment " + i);

        String expName = expNameFormat.format(i);

        Array array = generator.generate(i, accepter);
        MCFSFrameworkThreads mcfs = new MCFSClassicThreads();

        mcfs.mcfsArrays.sourceArray = array;
        mcfs.experimentName = initialSeed + "_" + expName;

        mcfs.mcfsParams = new MCFSParams();
        mcfs.mcfsParams.setDefault();
        mcfs.mcfsParams.inputFileName = mcfs.experimentName;
        mcfs.mcfsParams.tempFilesPATH = tmp.getAbsolutePath() + File.separator;
        mcfs.mcfsParams.showDistanceProgress = false;
        mcfs.mcfsParams.splits = 5;
        mcfs.mcfsParams.projections = projections;
        mcfs.mcfsParams.projectionSize = 0.05f;
        mcfs.mcfsParams.splitRatio = 0.66f;
        mcfs.mcfsParams.balanceClasses = false;
        mcfs.mcfsParams.balanceRatio = 3;
        mcfs.mcfsParams.splitSetSizeLimit = false;
        mcfs.mcfsParams.splitSetSize = 0;
        mcfs.mcfsParams.cutPointRuns = 30;
        mcfs.mcfsParams.threadsNumber = Runtime.getRuntime().availableProcessors();

        mcfs.run();

        AttributesImportance ai = mcfs.globalStats.getAttrImportances()[0];
        for (int m = 0; m < ai.getMeasuresNumber(); m++) {
          Ranking rank = ai.getTopSetRanking(m, generator.getColumns());
          String[] names = rank.getAttributesNames();
          String[] allLine = new String[names.length + 2];
          allLine[0] = String.valueOf(i);
          allLine[1] = rank.getMeasureName();
          System.arraycopy(names, 0, allLine, 2, names.length);
          all.writeNext(allLine);

          String[] sigLine = new String[generator.getLeftShifted() + generator.getRightShifted() + 2];
          sigLine[0] = String.valueOf(i);
          sigLine[1] = rank.getMeasureName();

          int index = 2;
          for (int k = 0; k < names.length; k++) {
            if (!StringUtils.startsWith(names[k], "V_N01_")) {
              sigLine[index] = String.valueOf(k);
              index++;
            }
          }
          significant.writeNext(sigLine);
        }
        all.flush();
        significant.flush();
      }
    } finally {
      try {
        all.close();
      } finally {
        significant.close();
      }
    }
  }

  private static void printUsage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(MCFSRunner.class.getName(), options);
  }

  private static Options createOptions() {
    Options options = new Options();
    options.addOption(OptionBuilder.withArgName("seed").hasArg().withType(PatternOptionBuilder.NUMBER_VALUE)
        .withDescription("generator seed (default to system time)").create("seed"));
    options.addOption(OptionBuilder.withArgName("start").hasArg().withType(PatternOptionBuilder.NUMBER_VALUE)
        .withDescription("start value (default to 1)").withLongOpt("start").create("start"));
    options.addOption(OptionBuilder.withArgName("end").hasArg().withType(PatternOptionBuilder.NUMBER_VALUE)
        .withDescription("end value (default to 100)").withLongOpt("end").create("end"));
    options.addOption(OptionBuilder.withArgName("projections").hasArg().withType(PatternOptionBuilder.NUMBER_VALUE)
        .withDescription("projections (default to 4000)").withLongOpt("projections").create());

    return options;
  }
}
