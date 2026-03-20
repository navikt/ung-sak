package no.nav.ung.ytelse.ungdomsprogramytelsen.beregning.barnetillegg;

public record Barnetillegg(int dagsats, int antallBarn) {

    @Override
    public String toString() {
        return "Barnetillegg{" +
            "dagsats=" + dagsats +
            ", antallBarn=" + antallBarn +
            '}';
    }

}
