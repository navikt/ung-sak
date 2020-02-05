package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;

import no.nav.k9.sak.typer.AktørId;

public class AvklartDataBarnAdapter {

    private AktørId aktørId;
    private LocalDate fødselsdato;
    private Integer nummer;

    public AvklartDataBarnAdapter(AktørId aktørId, LocalDate fødselsdato, Integer nummer) {
        this.aktørId = aktørId;
        this.fødselsdato = fødselsdato;
        this.nummer = nummer;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public Integer getNummer() {
        return nummer;
    }
}
