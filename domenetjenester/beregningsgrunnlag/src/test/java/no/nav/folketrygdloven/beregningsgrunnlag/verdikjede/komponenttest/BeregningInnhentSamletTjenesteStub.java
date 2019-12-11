package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.komponenttest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.AktoerIder;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumer;

@ApplicationScoped
class BeregningInnhentSamletTjenesteStub {

    @Inject
    public BeregningInnhentSamletTjenesteStub() {
        // empty constructor
    }

    static class AktørConsumerMock implements AktørConsumer {

        @Inject
        public AktørConsumerMock() {
        }

        @Override
        public Optional<String> hentAktørIdForPersonIdent(String personIdent) {
            return Optional.empty();
        }

        @Override
        public Optional<String> hentPersonIdentForAktørId(String aktørId) {
            return Optional.empty();
        }

        @Override
        public List<AktoerIder> hentAktørIdForPersonIdentSet(Set<String> set) {
            return Collections.emptyList();
        }
    }

    static class TpsTjenesteMock implements PersonIdentTjeneste {
        private static final PersonIdent PERSON_IDENT = PersonIdent.fra("12125612345");

        @Override
        public Optional<Personinfo> hentBrukerForAktør(AktørId aktørId) {
            return Optional.empty();
        }

        @Override
        public PersonIdent hentFnrForAktør(AktørId aktørId) {
            return PERSON_IDENT;
        }

        @Override
        public Optional<AktørId> hentAktørForFnr(PersonIdent fnr) {
            return Optional.empty();
        }

    }
}
