package INITAL_MANAGER;

import java.io.Serializable;

public class Agent_List_Objekt implements Serializable {

    private static final long serialVersionUID = 2493329449649334091L;

    private int agent_id = 0;

    private String agent_name = "";

    private String agent_datei_filter = "";

    private boolean activ = false;

    private boolean isLinux = false;

    public Agent_List_Objekt() {

    }

    public Agent_List_Objekt(int _agent_id, String _agent_name, String _agent_datei_filter, boolean _activ,
	    boolean _isLinux) {

	this.agent_id = _agent_id;
	this.agent_name = _agent_name;
	this.agent_datei_filter = _agent_datei_filter;
	this.activ = _activ;
	this.isLinux = _isLinux;
	

    }

    public int getAgent_id() {
	return agent_id;
    }

    public void setAgent_id(int agent_id) {
	this.agent_id = agent_id;
    }

    public String getAgent_name() {
	return agent_name;
    }

    public void setAgent_name(String agent_name) {
	this.agent_name = agent_name;
    }

    public String getAgent_datei_filter() {
	return agent_datei_filter;
    }

    public void setAgent_datei_filter(String agent_datei_filter) {
	this.agent_datei_filter = agent_datei_filter;
    }

    public boolean isActiv() {
	return activ;
    }

    public void setActiv(boolean activ) {
	this.activ = activ;
    }

    public boolean isLinux() {
	return isLinux;
    }

    public void setLinux(boolean isLinux) {
	this.isLinux = isLinux;
    }
}
