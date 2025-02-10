package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import java.math.BigDecimal;

public record BeregnetSats(BigDecimal grunnsats, int barnetilleggSats) {

    public BeregnetSats {
        if (grunnsats == null) {
            throw new IllegalArgumentException("grunnsats kan ikke være null");
        }
        if (grunnsats.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("grunnsats kan ikke være negativ");
        }
    }

    public static final BeregnetSats ZERO = new BeregnetSats(BigDecimal.ZERO, 0);

    public BeregnetSats adder(BeregnetSats sats) {
        return new BeregnetSats(this.grunnsats().add(sats.grunnsats()), this.barnetilleggSats() + sats.barnetilleggSats());
    }

    public BeregnetSats multipliser(int faktor) {
        return new BeregnetSats(this.grunnsats().multiply(BigDecimal.valueOf(faktor)), this.barnetilleggSats() * faktor);
    }

    public BigDecimal totalSats() {
        return grunnsats().add(BigDecimal.valueOf(barnetilleggSats()));
    }


}
