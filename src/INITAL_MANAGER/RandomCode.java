package INITAL_MANAGER;

import java.util.Random;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;

public class RandomCode implements java.io.Serializable {

    private static final long serialVersionUID = 4567345410418526300L;
    private int laenge = 0;

    public RandomCode() {

    }

    public RandomCode(int laenge) {

	this.laenge = laenge;

    }

    public String newRandomCode() {
	if (laenge <= 0) {
	    laenge = 10;
	}

	SecureRandom Srandom = new SecureRandom();

	Random ran = new Random();
	long real = 1234567890;

	StringBuilder StringB = new StringBuilder();

	while (StringB.length() < this.laenge + 20) {
	    real = real + ran.nextLong();

	    StringB.append(Long.toString(Math.abs(real), 36) + new BigInteger(130, Srandom).toString(32) + ""
		    + UUID.randomUUID().toString());

	}

	return StringB.toString().toUpperCase().substring(0, this.laenge).replace("-", "");
    }

    public String newRandomCodeZahlen() {
	if (laenge <= 0) {
	    laenge = 10;
	}
	int c;
	Random t = new Random();

	StringBuilder StringB = new StringBuilder();

	for (c = 1; c <= 10; c++) {

	    StringB.append(t.nextInt(99999));

	}
	return StringB.toString().toUpperCase().substring(0, this.laenge).replace("-", "");
    }

}
