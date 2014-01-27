package dmlabexec;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;

import dmLab.attrProcessing.Attribute;
import dmLab.container.array.Array;

public class Generator {

  private long initialSeed = 0;
  private int columns = 10000;
  private int observations = 100;
  private int leftShifted = 50;
  private int rightShifted = 50;
  private float shift = 1.0f;

  public Array generate(int experiment, SeedAccepter seedAccepter) throws Exception {
    // ustalamy liczbe eksperymentow, zmiennych i obserwacji
    int n01columns = columns - leftShifted - rightShifted;

    // inicjujemy generatory dla kolumn
    RandomGenerator[] generators = new RandomGenerator[columns];
    for (int i = 0; i < columns; i++) {
      long start = initialSeed + experiment * columns + i;
      generators[i] = new Well44497b(start);
      seedAccepter.acceptSeed(start);
    }

    // tworzymy kontener dla danych
    Array array = new Array();
    array.init(columns + 1, observations);

    array.attributes = new Attribute[columns + 1];

    // pierwsza (zerowa) kolumna - class
    array.attributes[0] = new Attribute();
    array.attributes[0].name = "Class";
    array.attributes[0].type = Attribute.NOMINAL;

    array.nextAttribute();

    for (int i = 1; i <= n01columns; i++) {
      array.attributes[i] = new Attribute();
      array.attributes[i].name = "V_N01_" + i;
      array.attributes[i].type = Attribute.NUMERIC;
      array.nextAttribute();
    }
    // rozklad N(0, L)
    for (int i = n01columns + 1; i <= n01columns + leftShifted; i++) {
      array.attributes[i] = new Attribute();
      array.attributes[i].name = "V_LS_" + i;
      array.attributes[i].type = Attribute.NUMERIC;
      array.nextAttribute();
    }
    // rozklad N(0, R)
    for (int i = n01columns + leftShifted + 1; i <= columns; i++) {
      array.attributes[i] = new Attribute();
      array.attributes[i].name = "V_RS_" + i + "";
      array.attributes[i].type = Attribute.NUMERIC;
      array.nextAttribute();
    }

    Set<String> decisionValues = new HashSet<>();
    for (int observation = 0; observation < observations; observation++) {
      String value = String.valueOf(observation % 2);
      decisionValues.add(value);
      array.writeStrValue(0, observation, value);
    }

    // kolumny z danymi
    // rozklad N(0,1) - nieistotne
    for (int i = 1; i <= n01columns; i++) {
      for (int observation = 0; observation < observations; observation++) {
        array.writeValue(i, observation, (float) generators[i - 1].nextGaussian());
      }
    }
    // rozklad N(0, L)
    for (int i = n01columns + 1; i <= n01columns + leftShifted; i++) {
      for (int observation = 0; observation < observations; observation++) {
        if ((observation % 2) == 0) {
          array.writeValue(i, observation, (float) generators[i - 1].nextGaussian() - getShift());
        } else {
          array.writeValue(i, observation, (float) generators[i - 1].nextGaussian());
        }
      }
    }
    // rozklad N(0, R)
    for (int i = n01columns + leftShifted + 1; i <= columns; i++) {
      for (int observation = 0; observation < observations; observation++) {
        if ((observation % 2) == 0) {
          array.writeValue(i, observation, (float) generators[i - 1].nextGaussian());
        } else {
          array.writeValue(i, observation, (float) generators[i - 1].nextGaussian() + getShift());
        }
      }
    }

    array.setDecisionAttr(0);

    float[] decValues = new float[decisionValues.size()];
    int counter = 0;
    for (String value : decisionValues) {
      decValues[counter] = array.dictionary.toFloat(value);
      counter++;
    }
    array.setDecValues(decValues);

    for (int i = 0; i < observations; i++) {
      array.nextEvent();
    }

    return array;
  }

  public int getColumns() {
    return columns;
  }

  public void setColumns(int columns) {
    this.columns = columns;
  }

  public int getObservations() {
    return observations;
  }

  public void setObservations(int observations) {
    this.observations = observations;
  }

  public int getLeftShifted() {
    return leftShifted;
  }

  public void setLeftShifted(int leftShifted) {
    this.leftShifted = leftShifted;
  }

  public int getRightShifted() {
    return rightShifted;
  }

  public void setRightShifted(int rightShifted) {
    this.rightShifted = rightShifted;
  }

  public float getShift() {
    return shift;
  }

  public void setShift(float shift) {
    this.shift = shift;
  }

  public long getInitialSeed() {
    return initialSeed;
  }

  public void setInitialSeed(long initialSeed) {
    this.initialSeed = initialSeed;
  }
}
