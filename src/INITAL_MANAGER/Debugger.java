package INITAL_MANAGER;

import java.text.SimpleDateFormat;

public class Debugger implements java.io.Serializable {

    private static final long serialVersionUID = -9183304675047880313L;

    public boolean debugStatus = false;
    public StringBuilder debugString = null;

    SimpleDateFormat format2 = new SimpleDateFormat("dd.MM.yyyy 'um' HH:mm:ss");

    public Debugger(boolean _debugstatus) {

	debugStatus = _debugstatus;
	debugString = new StringBuilder();
    }

    public Debugger() {

	debugString = new StringBuilder();
    }

    public void showDebbugInfo() {

	if (this.debugStatus == true) {

	    System.out
		    .println("<S----------------------------------DEBUG-------------------------------------------->");
	    System.out.println(debugString.toString());
	    System.out
		    .println("<E----------------------------------DEBUG-------------------------------------------->");
	}

    }

    public void addMassage(String info) {

	debugString.append("-|> " + info + "\n");
	System.out.println("-|> " + info);

    }

}
