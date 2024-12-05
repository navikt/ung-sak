package no.nav.ung.sak.ytelse.ung.hendelsemottak;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.Doedsfall;
import no.nav.k9.felles.integrasjon.pdl.Foedselsdato;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.k9.felles.integrasjon.pdl.Person;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;

@ApplicationScoped
@HendelseTypeRef("PDL_DØDSFALL")
public class PdlDødsfallFagsakTilVurderingUtleder extends PdlHendelseFagsakTilVurderingUtleder {

    public PdlDødsfallFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public PdlDødsfallFagsakTilVurderingUtleder(FagsakRepository fagsakRepository,
                                                BehandlingRepository behandlingRepository,
                                                UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, PdlKlient pdlKlient) {
        super(fagsakRepository, behandlingRepository, ungdomsprogramPeriodeRepository, pdlKlient);
    }

    LocalDate finnAktuellDato(Person personFraPdl) {
        return personFraPdl.getDoedsfall().stream()
            .map(Doedsfall::getDoedsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
    }

    BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER;
    }

}
