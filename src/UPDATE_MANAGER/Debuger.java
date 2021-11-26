package UPDATE_MANAGER;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Debuger implements java.io.Serializable {

    private static final long serialVersionUID = -1925952354797216230L;

    boolean debugstatus = false;

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

	    System.out.println(debug_string.toString());

	}

    }

    public void addMassage(String info) {

	debug_string.append("--|> " + info + "\n");
	System.out.println("--|> " + info);

    }

}
