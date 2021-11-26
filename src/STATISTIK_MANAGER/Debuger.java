package STATISTIK_MANAGER;


import java.text.SimpleDateFormat;
import java.util.Date;

public class Debuger implements java.io.Serializable {

    private static final long serialVersionUID = -9183304675047880313L;

    public boolean debugstatus = false;

    SimpleDateFormat format2 = new SimpleDateFormat("dd.MM.yyyy 'um' HH:mm:ss");

    StringBuilder debug_string = null;

    public Debuger(boolean _debugstatus) {

	debugstatus = _debugstatus;
	debug_string = new StringBuilder();
    }

    public Debuger() {

	debug_string = new StringBuilder();
    }

    public void showDebbugInfo() {

	if (this.debugstatus == true) {

	    System.out
		    .println("<S----------------------------------DEBUG-------------------------------------------->");
	    System.out.println(debug_string.toString());
	    System.out
		    .println("<E----------------------------------DEBUG-------------------------------------------->");
	}

    }

    public void addMassage(String info) {

	Date adtime = new Date();

	debug_string.append("---|> " + info);
	System.out.println("---|> " + info);

    }

}
