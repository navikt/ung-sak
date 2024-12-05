package no.nav.ung.sak.ytelse.ung.hendelsemottak;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.DoedsfallResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Foedselsdato;
import no.nav.k9.felles.integrasjon.pdl.FoedselsdatoResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjon;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonRolle;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkResult;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkResultResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.HentPersonQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.IdentGruppe;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjon;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.k9.felles.integrasjon.pdl.Person;
import no.nav.k9.felles.integrasjon.pdl.PersonResponseProjection;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;

@ApplicationScoped
@HendelseTypeRef("PDL_FØDSEL")
public class PdlFødselFagsakTilVurderingUtleder extends PdlHendelseFagsakTilVurderingUtleder {

    public PdlFødselFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public PdlFødselFagsakTilVurderingUtleder(FagsakRepository fagsakRepository,
                                              BehandlingRepository behandlingRepository,
                                              UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, PdlKlient pdlKlient) {
        super(fagsakRepository, behandlingRepository, ungdomsprogramPeriodeRepository, pdlKlient);
    }

    LocalDate finnAktuellDato(Person personFraPdl) {
        return personFraPdl.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
    }

}
