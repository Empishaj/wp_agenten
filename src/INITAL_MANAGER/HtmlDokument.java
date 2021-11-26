package INITAL_MANAGER;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class HtmlDokument implements java.io.Serializable {

    private static final long serialVersionUID = -8831288069117089759L;

    StringBuffer headHtml = new StringBuffer();
    StringBuffer bodyHtml = new StringBuffer();
    StringBuffer footerHtml = new StringBuffer();

    SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    SimpleDateFormat format2 = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

    StringBuffer gesamt = new StringBuffer();

    public HtmlDokument() {

    }

    public StringBuffer generiereHTMLInhalt(Vector<AnalyseArtikel> diffArtikel) {

	String Datum = format1.format(new Date());

	headHtml.append("<!DOCTYPE html>");
	headHtml.append("<html lang=\"de\">");
	headHtml.append("<head>");
	headHtml.append("<title>WPLAG-Analyse: Datenbestand von " + Datum + "</title>");
	headHtml.append("<style>");

	headHtml.append(".datagrid table { border-collapse: collapse; text-align: left; width: 100%;}");
	headHtml.append(
		".datagrid {font: normal 12px/150% Courier New, Courier, monospace;background: #fff;overflow: hidden;border: 5px solid #000000;-webkit-border-radius: 3px;-moz-border-radius: 3px;border-radius: 3px;width: 80%;margin: 0px auto; min-height: 46px;text-align: center;}");
	headHtml.append(".datagrid table td, .datagrid table th { padding: 3px 10px; }");
	headHtml.append(
		".datagrid table thead th {background-color: #ffffff;color: #000000;font-size:15px;font-weight: bold;border-left: 5px solid #000000;border-bottom: 5px solid #000000;text-align: center;}");
	headHtml.append(
		".datagrid table tbody td {color: #000000; font-size: 14px;border-bottom: 1px solid #000000;font-weight: normal; text-align: center;}");
	headHtml.append(".datagrid table tbody .alt td { background: #DFFFDE; color: #275420; }");
	headHtml.append(".datagrid table tbody td:first-child { border-left: none; }");
	headHtml.append(".datagrid table tbody tr:last-child td { border-bottom: none; }");
	headHtml.append(".datagrid table tbody tr:last-child td { border-bottom: none; }");
	headHtml.append(".datagrid table tfoot td div { border-top: 1px solid #36752D;background: #DFFFDE;} ");
	headHtml.append(".datagrid table tfoot td { padding: 0; font-size: 13px } ");
	headHtml.append(".datagrid table tfoot td div{ padding: 2px; } ");
	headHtml.append(".datagrid table tfoot td ul { margin: 0; padding:0; list-style: none; text-align: right; }");
	headHtml.append(".datagrid table tfoot  li { display: inline; }");
	headHtml.append(
		".datagrid table tfoot li a { text-decoration: none; display: inline-block;  padding: 2px 8px; margin: 1px;color: #FFFFFF;border: 1px solid #36752D;-webkit-border-radius: 3px; -moz-border-radius: 3px; border-radius: 3px; background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #36752D), color-stop(1, #275420) );background:-moz-linear-gradient( center top, #36752D 5%, #275420 100% );filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#36752D', endColorstr='#275420');background-color:#36752D; }");
	headHtml.append(
		".datagrid table tfoot ul.active, .datagrid table tfoot ul a:hover { text-decoration: none;border-color: #275420; color: #FFFFFF; background: none; background-color:#36752D;}div.dhtmlx_window_active, div.dhx_modal_cover_dv { position: fixed !important; }");
	headHtml.append(".rightBirder{border-right: 5px solid #000000;}");

	headHtml.append("</style>");

	headHtml.append("<script src=\"https://code.jquery.com/jquery-1.12.4.js\"></script>");
	headHtml.append("<script src=\"https://code.jquery.com/ui/1.12.1/jquery-ui.js\"></script>");

	headHtml.append("</head>");

	headHtml.append("<body>");

	headHtml.append("<div  >");
	headHtml.append("<div style=\"width:100%;margin-bottom: 50px;text-align: center;display: table;\">");
	headHtml.append(
		"<span style=\"font: normal 30px/59% Courier New, Courier, monospace;margin-top: 20px;position: relative;top: 20px; font-weight: bold;\"> Analyse Datenbestand vom "
			+ format1.format(new Date()) + "</span>");

	headHtml.append("</div>");

	headHtml.append("<div class=\"datagrid\">");

	if (diffArtikel.size() > 0) {

	    headHtml.append("<table style=\"width: 100%; position: relative; text-align:left\">");

	    headHtml.append("<thead>");
	    headHtml.append("<tr>");
	    headHtml.append("<th style=\"border-left: none;\">Nr.</th>");
	    headHtml.append("<th >Artikel-Nummer</th>");
	    headHtml.append("<th>Lager [ALT] </th>");
	    headHtml.append("<th>Lager [NEU] </th>");
	    headHtml.append("<th>DIFF-L </th> ");

	    headHtml.append("<th>Reserviert Auftrag [ALT] </th> ");
	    headHtml.append("<th>Reserviert Auftrag [NEU] </th> ");
	    headHtml.append("<th>DIFF-AR</th> ");

	    headHtml.append("<th>Reserviert Rechnung [ALT] </th> ");
	    headHtml.append("<th>Reserviert Rechnung [NEU] </th> ");
	    headHtml.append("<th>DIFF-RR</th> ");

	    headHtml.append("<th>Bestellt [ALT]</th> ");
	    headHtml.append("<th>Bestellt [NEU] </th>");
	    headHtml.append("<th>DIFF-B</th> ");
	    headHtml.append("</tr>");
	    headHtml.append("</thead>");
	    headHtml.append("<tbody>");

	    for (int i = 0; i < diffArtikel.size(); i++) {

		bodyHtml.append("<tr>");
		bodyHtml.append("<td class=\"rightBirder\"> " + (i + 1) + "  </td>");

		bodyHtml.append("<td class=\"rightBirder\"> " + diffArtikel.get(i).getArtikel_nummer() + "</td>");

		if (diffArtikel.get(i).getLager_diff() > 0 || diffArtikel.get(i).getLager_diff() < 0) {
		    bodyHtml.append("<td style=\"background-color: rgba(247, 31, 31, 0.4);\" >"
			    + diffArtikel.get(i).getLager_alt() + "</td>");
		    bodyHtml.append("<td  style=\"background-color:rgba(247, 31, 31, 0.4);\" class=\"rightBirder\">"
			    + diffArtikel.get(i).getLager_neu() + "</td>");
		    bodyHtml.append("<td class=\"rightBirder\" style=\"color:red!important;\">"
			    + diffArtikel.get(i).getLager_diff() + "</td>");
		} else {

		    bodyHtml.append("<td>" + diffArtikel.get(i).getLager_alt() + "</td>");
		    bodyHtml.append("<td class=\"rightBirder\">" + diffArtikel.get(i).getLager_neu() + "</td>");

		    bodyHtml.append("<td class=\"rightBirder\">" + diffArtikel.get(i).getLager_diff() + "</td>");

		}

		if (diffArtikel.get(i).getReserviert_diff() > 0 || diffArtikel.get(i).getReserviert_diff() < 0) {

		    bodyHtml.append("<td style=\"background-color: rgba(247, 31, 31, 0.4);\" >"
			    + diffArtikel.get(i).getReserviert_alt() + "</td>");
		    bodyHtml.append("<td style=\"background-color: rgba(247, 31, 31, 0.4);\" class=\"rightBirder\">"
			    + diffArtikel.get(i).getReserviert_neu() + "</td> ");

		    bodyHtml.append("<td style=\"color:red!important;\"  class=\"rightBirder\"  >"
			    + diffArtikel.get(i).getReserviert_diff() + "</td>");

		} else {

		    bodyHtml.append("<td >" + diffArtikel.get(i).getReserviert_alt() + "</td>");
		    bodyHtml.append("<td class=\"rightBirder\">" + diffArtikel.get(i).getReserviert_neu() + "</td> ");
		    bodyHtml.append("<td class=\"rightBirder\">" + diffArtikel.get(i).getReserviert_diff() + "</td>");

		}

		if (diffArtikel.get(i).getRech_diff() > 0 || diffArtikel.get(i).getRech_diff() < 0) {

		    bodyHtml.append("<td style=\"background-color: rgba(247, 31, 31, 0.4);\" >"
			    + diffArtikel.get(i).getReserviert_rech_alt() + "</td>");
		    bodyHtml.append("<td style=\"background-color: rgba(247, 31, 31, 0.4);\" class=\"rightBirder\">"
			    + diffArtikel.get(i).getReserviert_rech_neu() + "</td> ");

		    bodyHtml.append("<td style=\"color:red!important;\"  class=\"rightBirder\"  >"
			    + diffArtikel.get(i).getRech_diff() + "</td>");

		} else {

		    bodyHtml.append("<td >" + diffArtikel.get(i).getReserviert_rech_alt() + "</td>");
		    bodyHtml.append(
			    "<td class=\"rightBirder\">" + diffArtikel.get(i).getReserviert_rech_neu() + "</td> ");
		    bodyHtml.append("<td class=\"rightBirder\">" + diffArtikel.get(i).getRech_diff() + "</td>");

		}

		if (diffArtikel.get(i).getBestellt_diff() > 0 || diffArtikel.get(i).getBestellt_diff() < 0) {

		    bodyHtml.append("<td style=\"background-color:rgba(247, 31, 31, 0.4);\" >"
			    + diffArtikel.get(i).getBestellt_alt() + "</td>");
		    bodyHtml.append("<td style=\"background-color: rgba(247, 31, 31, 0.4);\" class=\"rightBirder\">"
			    + diffArtikel.get(i).getBestellt_neu() + "</td>");

		    bodyHtml.append(
			    "<td style=\"color:red!important;\" >" + diffArtikel.get(i).getBestellt_diff() + " </td>");

		} else {
		    bodyHtml.append("<td>" + diffArtikel.get(i).getBestellt_alt() + "</td>");
		    bodyHtml.append("<td class=\"rightBirder\">" + diffArtikel.get(i).getBestellt_neu() + "</td>");
		    bodyHtml.append("<td >" + diffArtikel.get(i).getBestellt_diff() + "</td>");

		}

		bodyHtml.append("</tr>");

	    }

	    bodyHtml.append(" </tbody>");
	    bodyHtml.append("</table>");

	} else {

	    bodyHtml.append(
		    "<span style=\"font: normal 25px/59% Courier New, Courier, monospace;margin-top: 20px;position: relative;top: 20px; font-weight: bold;\" >Es wurden keine Differenzen gefunden.</span>");

	}

	bodyHtml.append("</div>");
	bodyHtml.append("</div>");

	bodyHtml.append("</body>");
	bodyHtml.append("</html>");

	footerHtml.append("");

	gesamt.append(headHtml.toString());
	gesamt.append(bodyHtml.toString());
	gesamt.append(footerHtml.toString());

	return gesamt;

    }

}
