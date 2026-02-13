package no.nav.ung.ytelse.aktivitetspenger.beregning.barnetillegg;

public record Barnetillegg(int dagsats, int antallBarn) {

    @Override
    public String toString() {
        return "Barnetillegg{" +
            "dagsats=" + dagsats +
            ", antallBarn=" + antallBarn +
            '}';
    }

}
