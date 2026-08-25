package no.nav.ung.sak.domene.arbeidsforhold.person;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.ung.sak.typer.AktørId;

import java.util.Optional;

public interface PersonIdentTjeneste {

    Optional<PersoninfoArbeidsgiver> hentPersoninfoArbeidsgiver(AktørId aktørId, FagsakYtelseType ytelseType);

}
