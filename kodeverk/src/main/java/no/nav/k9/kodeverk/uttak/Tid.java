package no.nav.k9.kodeverk.uttak;

import java.time.LocalDate;
import java.time.Month;

public class Tid {

    // konstanter kopiert inn fra felles-util for å slippe avhengighet til andre biblioteker i denne modulen.
    // 
    
    public static final LocalDate TIDENES_BEGYNNELSE = LocalDate.of(-4712, Month.JANUARY, 1);
    public static final LocalDate TIDENES_ENDE = LocalDate.of(9999, Month.DECEMBER, 31);
}
