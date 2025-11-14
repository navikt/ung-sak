package no.nav.ung.sak.kontroll;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Beløp;

public record Inntektsperiode(
    Beløp beløp,
    Beløp periodebeløpFraRådata,
    DatoIntervallEntitet periode
) {


    public Inntektsperiode adderBeløp(Inntektsperiode augend) {
        if (!this.periode.equals(augend.periode)) {
            throw new IllegalArgumentException("Kan kun addere inntektsperioder med samme periode");
        }
        return new Inntektsperiode(
            this.beløp.adder(augend.beløp),
            this.periodebeløpFraRådata.adder(augend.periodebeløpFraRådata),
            this.periode
        );
    }

}
