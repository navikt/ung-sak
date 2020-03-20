package no.nav.k9.sak.domene.person.personopplysning;

import java.time.LocalDate;

import no.nav.k9.sak.typer.AktørId;

public class AvklartDataForeldreAdapter {

    private AktørId aktørId;
    private LocalDate dødsdato;

    public AvklartDataForeldreAdapter(AktørId aktørId, LocalDate dødsdato) {
        this.aktørId = aktørId;
        this.dødsdato = dødsdato;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }
}
