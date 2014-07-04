package dmlabexec;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import au.com.bytecode.opencsv.CSVWriter;
import dmLab.attrSelection.MCFSParams;
import dmLab.attrSelection.attrImportance.AttributesImportance;
import dmLab.attrSelection.attrImportance.Ranking;
import dmLab.attrSelection.mcfsEngine.framework.MCFSClassicThreads;
import dmLab.attrSelection.mcfsEngine.framework.MCFSFrameworkThreads;
import dmLab.container.array.Array;
import dmLab.container.saver.Array2File;

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
    int end = Integer.MAX_VALUE;
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
    float shift = 1.0f;
    if (line.hasOption("shift")) {
      shift = ((Number) line.getParsedOptionValue("shift")).floatValue();
    }

    boolean calculate = true;
    if (line.hasOption("skip")) {
      calculate = false;
    }

    float contrastRatio = 0;
    if (line.hasOption("contastRatio")) {
      contrastRatio = ((Number) line.getParsedOptionValue("contastRatio")).floatValue();
    }

    MVariantProfile profile = null;
    if (line.hasOption("profile")) {
      profile = MVariantProfile.valueOf(line.getOptionValue("profile"));
    }

    String opis = null;
    Generator generator = null;
    if (profile != null) {
      generator = new MVariantGenerator(profile);
      opis = "\tstart:\t" + start + "\n\tend:\t" + end + "\n\tseed:\t" + initialSeed
          + "\n\tprojections:\t" + projections + "\n\tcontrast ratio:\t" + contrastRatio
          + "\n\tprofile:\t" + profile;
    } else {
      opis = "\tstart:\t" + start + "\n\tend:\t" + end + "\n\tseed:\t" + initialSeed
          + "\n\tprojections:\t" + projections + "\n\tshift:\t" + shift + "\n\tcontrast ratio:\t" + contrastRatio;
      RegularGenerator rgenerator = new RegularGenerator();
      rgenerator.setInitialSeed(initialSeed);
      rgenerator.setSeedAccepter(new DuplicateCheck());
      rgenerator.setShift(shift);
      generator = rgenerator;
    }

    System.out.println("Running\n" + opis);
    NumberFormat expNameFormat = new DecimalFormat(StringUtils.repeat("0", String.valueOf(end).length()));

    String tst = DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd-HH-mm");


    File tmpRoot = new File("tmp" + File.separator + tst);
    if (!tmpRoot.exists()) {
      tmpRoot.mkdirs();
    }
    File tmp = new File(tmpRoot, String.valueOf(initialSeed));
    tmp.mkdirs();

    File daneRoot = new File("dane" + File.separator + tst);
    if (!daneRoot.exists()) {
      daneRoot.mkdirs();
    }
    File dane = new File(daneRoot, String.valueOf(initialSeed));
    dane.mkdirs();

    try (FileWriter writer = new FileWriter(new File(dane, "opis.txt"))) {
      writer.write(opis);
    }

    CSVWriter all = new CSVWriter(new FileWriter(new File(dane, "exp_" + initialSeed + "_" + tst + "_all.csv")), CSVWriter.DEFAULT_SEPARATOR,
        CSVWriter.NO_QUOTE_CHARACTER);

    try {
      for (int i = start; i <= end; i++) {
        System.out.println("experiment " + i);

        File tmpDir = new File(tmp, String.valueOf(i));
        tmpDir.mkdirs();

        String expName = expNameFormat.format(i);

        Array array = generator.generate(i);

        NumberFormat formatter = new DecimalFormat("00000");

        Array2File array2File = new Array2File();
        array2File.saveFile(array, dane.getAbsolutePath() + File.separator + "dane_" + formatter.format(i) + ".arff");

        if (calculate) {
          MCFSFrameworkThreads mcfs = new MCFSClassicThreads();

          mcfs.mcfsArrays.sourceArray = array;
          mcfs.experimentName = initialSeed + "_" + expName;

          mcfs.mcfsParams = new MCFSParams();
          mcfs.mcfsParams.setDefault();
          mcfs.mcfsParams.inputFileName = mcfs.experimentName;
          mcfs.mcfsParams.tempFilesPATH = tmpDir.getAbsolutePath() + File.separator;
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

          mcfs.mcfsParams.contrastAttrRatio = contrastRatio;

          mcfs.mcfsParams.threadsNumber = Runtime.getRuntime().availableProcessors();

          mcfs.run();

          try {
            File[] files = tmpDir.listFiles();
            for (File file : files) {
              file.delete();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          AttributesImportance ai = mcfs.globalStats.getAttrImportances()[0];
          for (int m = 0; m < ai.getMeasuresNumber(); m++) {
            int columns = generator.getColumns();
            if (contrastRatio > 0) {
              columns = columns * 2;
            }
            Ranking rank = ai.getTopSetRanking(m, columns);
            String[] names = rank.getAttributesNames();
            String[] allLine = new String[names.length + 2];
            allLine[0] = String.valueOf(i);
            allLine[1] = rank.getMeasureName();
            System.arraycopy(names, 0, allLine, 2, names.length);
            all.writeNext(allLine);
          }
          all.flush();
        }
      }
    } finally {
      all.close();
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
    options.addOption(OptionBuilder.withArgName("shift").hasArg().withType(PatternOptionBuilder.NUMBER_VALUE)
        .withDescription("shift)").create("shift"));
    options.addOption(OptionBuilder.withArgName("start").hasArg().withType(PatternOptionBuilder.NUMBER_VALUE)
        .withDescription("start value (default to 1)").withLongOpt("start").create("start"));
    options.addOption(OptionBuilder.withArgName("end").hasArg().withType(PatternOptionBuilder.NUMBER_VALUE)
        .withDescription("end value (default to 100)").withLongOpt("end").create("end"));
    options.addOption(OptionBuilder.withArgName("projections").hasArg().withType(PatternOptionBuilder.NUMBER_VALUE)
        .withDescription("projections (default to 4000)").withLongOpt("projections").create());

    options.addOption(OptionBuilder.withArgName("contastRatio").hasArg().withType(PatternOptionBuilder.NUMBER_VALUE)
        .withDescription("contastRatio (default to 0)").create("contastRatio"));

    options.addOption(OptionBuilder.withArgName("profile").hasArg().withType(PatternOptionBuilder.STRING_VALUE)
        .withDescription("MVariantProfile").create("profile"));

    options.addOption(OptionBuilder.withDescription("skip calculations, only create files").withLongOpt("skip")
        .create("skip"));

    return options;
  }
}
