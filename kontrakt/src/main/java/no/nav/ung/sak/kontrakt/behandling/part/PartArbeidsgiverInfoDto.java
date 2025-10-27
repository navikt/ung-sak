package no.nav.ung.sak.kontrakt.behandling.part;

import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.Identifikasjon;

import static no.nav.ung.sak.typer.RolleType.ARBEIDSGIVER;

public class PartArbeidsgiverInfoDto extends PartDto {

    public PartArbeidsgiverInfoDto(Identifikasjon identifikasjon) {
        super(identifikasjon, ARBEIDSGIVER);
    }

    public PartArbeidsgiverInfoDto(AktørId aktørId) {
        super(Identifikasjon.av(aktørId), ARBEIDSGIVER);
    }

    public static PartArbeidsgiverInfoDto av(Arbeidsgiver arbeidsgiver) {
        return new PartArbeidsgiverInfoDto(Identifikasjon.av(arbeidsgiver));
    }
}
