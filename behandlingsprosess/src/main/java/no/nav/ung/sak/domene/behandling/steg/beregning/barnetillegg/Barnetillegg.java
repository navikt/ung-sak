package no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg;

public record Barnetillegg(int dagsats, int antallBarn) {

    @Override
    public String toString() {
        return "Barnetillegg{" +
            "dagsats=" + dagsats +
            ", antallBarn=" + antallBarn +
            '}';
    }

}
