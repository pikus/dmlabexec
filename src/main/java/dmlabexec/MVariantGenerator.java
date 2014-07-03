package dmlabexec;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;

import dmLab.attrProcessing.Attribute;
import dmLab.container.array.Array;

public class MVariantGenerator implements Generator {

  private long initialSeed = 0;
  private int columns = 5000;
  private int shifted = 20;
  private MVariantProfile profile;

  public MVariantGenerator(MVariantProfile profile) {
    this.profile = profile;
  }

  @Override
  public Array generate(int experiment) {
    RandomGenerator generator = new Well44497b(initialSeed + experiment);
    Array result = new Array();

    result.init(columns + 1, 2 * profile.n());

    result.attributes = new Attribute[columns + 1];

    // pierwsza (zerowa) kolumna - class
    result.attributes[0] = new Attribute();
    result.attributes[0].name = "Class";
    result.attributes[0].type = Attribute.NOMINAL;

    result.nextAttribute();

    for (int i = 1; i <= shifted; i++) {
      result.attributes[i] = new Attribute();
      result.attributes[i].name = "V_S_" + i;
      result.attributes[i].type = Attribute.NUMERIC;
      result.nextAttribute();
    }

    for (int i = shifted + 1; i <= columns; i++) {
      result.attributes[i] = new Attribute();
      result.attributes[i].name = "V_N_" + i;
      result.attributes[i].type = Attribute.NUMERIC;
      result.nextAttribute();
    }

    Set<String> decisionValues = new HashSet<>(2);
    decisionValues.add("0");
    decisionValues.add("1");

    for (int observation = 0; observation < profile.n(); observation++) {
      result.writeStrValue(0, observation,"0");
    }
    for (int observation = profile.n(); observation < 2 * profile.n(); observation++) {
      result.writeStrValue(0, observation,"1");
    }

    double[][] covariances = buildCovariance(shifted, profile.ro());
    double[] a0 = buildA0(shifted, 0);
    double[] a1 = buildA0(shifted, profile.delta());
    MultivariateNormalDistribution mnd0 = new MultivariateNormalDistribution(generator, a0, covariances);
    MultivariateNormalDistribution mnd1 = new MultivariateNormalDistribution(generator, a1, covariances);

    // kolumny z danymi
    // kolumny istotne
    double[][] d0 = mnd0.sample(profile.n());
    for (int i = 0; i < shifted; i++) {
      for (int j = 0; j < profile.n(); j++) {
        result.writeValue(i + 1, j, (float) d0[j][i]);
      }
    }
    double[][] d1 = mnd1.sample(profile.n());
    for (int i = 0; i < shifted; i++) {
      for (int j = 0; j < profile.n(); j++) {
        result.writeValue(i + 1, j + profile.n(), (float) d1[j][i]);
      }
    }


    // rozklad N(0, 1)
    for (int i = shifted + 1; i <= columns; i++) {
      for (int observation = 0; observation < 2 * profile.n(); observation++) {
         result.writeValue(i, observation, (float) generator.nextGaussian());
      }
    }

    result.setDecisionAttr(0);

    float[] decValues = new float[decisionValues.size()];
    int counter = 0;
    for (String value : decisionValues) {
      decValues[counter] = result.dictionary.toFloat(value);
      counter++;
    }
    result.setDecValues(decValues);
    for (int i = 0; i < 2 * profile.n(); i++) {
      result.nextEvent();
    }

    return result;
  }

  private double[] buildA0(int dim, double ro) {
    double[] result = new double[dim];
    for (int i = 0; i < dim; i++) {
      result[i] = ro;
    }
    return result;
  }

  private double[][] buildCovariance(int dim, double ro) {
    double[][] result = new double[dim][dim];
    for (int i = 0; i < dim; i++) {
      for (int j = 0; j < dim; j++) {
        if (i == j) {
          result[i][j] = 1;
        } else {
          result[i][j] = ro;
        }
      }
    }
    return result;
  }

  public void setColumns(int columns) {
    this.columns = columns;
  }

  public void setShifted(int shifted) {
    this.shifted = shifted;
  }

  public void setInitialSeed(long initialSeed) {
    this.initialSeed = initialSeed;
  }

  @Override
  public int getColumns() {
    return columns;
  }

}
