package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.ung.sak.behandlingslager.behandling.sporing.Sporingsverdi;

import java.math.BigDecimal;
import java.util.Objects;

public final class BeregnetSats extends Sporingsverdi {

    public BeregnetSats(BigDecimal grunnsats, int barnetilleggSats) {
        if (grunnsats == null) {
            throw new IllegalArgumentException("grunnsats kan ikke være null");
        }
        if (grunnsats.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("grunnsats kan ikke være negativ");
        }
        this.grunnsats = grunnsats;
        this.barnetilleggSats = barnetilleggSats;
    }

    public static final BeregnetSats ZERO = new BeregnetSats(BigDecimal.ZERO, 0);
    private final BigDecimal grunnsats;
    private final int barnetilleggSats;


    public BeregnetSats adder(BeregnetSats sats) {
        return new BeregnetSats(this.grunnsats().add(sats.grunnsats()), this.barnetilleggSats() + sats.barnetilleggSats());
    }

    public BeregnetSats multipliser(int faktor) {
        return new BeregnetSats(this.grunnsats().multiply(BigDecimal.valueOf(faktor)), this.barnetilleggSats() * faktor);
    }

    public BigDecimal totalSats() {
        return grunnsats().add(BigDecimal.valueOf(barnetilleggSats()));
    }


    @Override
    public String tilRegelVerdi() {
        return toString();
    }


    @Override
    public String toString() {
        return "BeregnetSats{" +
            "grunnsats=" + grunnsats +
            ", barnetilleggSats=" + barnetilleggSats +
            '}';
    }

    public BigDecimal grunnsats() {
        return grunnsats;
    }

    public int barnetilleggSats() {
        return barnetilleggSats;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BeregnetSats) obj;
        return Objects.equals(this.grunnsats, that.grunnsats) &&
            this.barnetilleggSats == that.barnetilleggSats;
    }

    @Override
    public int hashCode() {
        return Objects.hash(grunnsats, barnetilleggSats);
    }

}
