package dmlabexec;

import dmLab.container.array.Array;

public interface Generator {

  Array generate(int experiment) throws Exception;

  int getColumns();
}
