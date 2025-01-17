package no.nav.ung.sak.hendelsemottak.tjenester;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.ung.sak.kontrakt.hendelser.HarFåttBarnHendelse;
import no.nav.ung.sak.typer.Periode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;

@ApplicationScoped
public class HendelsemottakTjeneste {

    private final Logger log = LoggerFactory.getLogger(HendelsemottakTjeneste.class);

    private Instance<FagsakerTilVurderingUtleder> utledere;

    private BehandlingRepository behandlingRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private PdlKlient pdlKlient;

    HendelsemottakTjeneste() {
        // CDI
    }

    @Inject
    public HendelsemottakTjeneste(@Any Instance<FagsakerTilVurderingUtleder> utledere,
                                  BehandlingRepository behandlingRepository,
                                  FagsakProsessTaskRepository fagsakProsessTaskRepository, PdlKlient pdlKlient) {
        this.utledere = utledere;
        this.behandlingRepository = behandlingRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.pdlKlient = pdlKlient;
    }

    private LocalDate utledDato(LocalDate fagsakSluttdato, Periode hendelsePeriode) {
        var hendelseDato = hendelsePeriode.getTom();
        if (hendelseDato.isBefore(fagsakSluttdato)) {
            return fagsakSluttdato;
        }
        return hendelseDato;
    }

    private List<FagsakerTilVurderingUtleder> finnMatchendeUtledere(HendelseType hendelseType) {
        var matchendeUtledere = HendelseTypeRef.Lookup.list(utledere, hendelseType.getKode());
        return new ArrayList<>(matchendeUtledere);
    }

    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(Hendelse hendelse) {
        var fagsakerMedBehandlingÅrsak = finnMatchendeUtledere(hendelse.getHendelseType())
            .stream()
            .map(utleder -> utleder.finnFagsakerTilVurdering(hendelse))
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return fagsakerMedBehandlingÅrsak;
    }

    public Map<Fagsak, BehandlingÅrsakType> mottaHendelse(Hendelse payload) {
        var kandidaterTilRevurdering = finnFagsakerTilVurdering(payload);

        List<String> saksnumre = kandidaterTilRevurdering.keySet().stream().map(f -> f.getSaksnummer().getVerdi()).toList();
        log.info("Mottok hendelse '{}', fant {} relevante fagsaker: {}", payload.getHendelseType(), saksnumre.size(), saksnumre);

        for (Map.Entry<Fagsak, BehandlingÅrsakType> entry : kandidaterTilRevurdering.entrySet()) {
            var fagsak = entry.getKey();
            var behandlingÅrsak = entry.getValue();

            ProsessTaskData tilRevurderingTaskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, behandlingÅrsak.getKode());

            Periode hendelsePeriode;
            if (payload instanceof HarFåttBarnHendelse) {
                HarFåttBarnHendelse harFåttBarnHendelse = (HarFåttBarnHendelse) payload;
                Person barnInfo = hentPersonInformasjon(harFåttBarnHendelse.getBarnIdent().getIdent());
                LocalDate aktuellDato = finnAktuellDato(barnInfo);
                hendelsePeriode = new Periode(aktuellDato, aktuellDato);
            } else {
                hendelsePeriode = payload.getHendelsePeriode();
            }

            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_FOM, hendelsePeriode.getFom().toString());
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_TOM, utledDato(fagsak.getPeriode().getTomDato(), hendelsePeriode).toString());
            var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
            if (sisteBehandling.isPresent()) {
                Behandling tilRevurdering = sisteBehandling.get();
                tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());
                fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
            } else {
                log.warn("Det var ingen behandling knyttet til {}, ignorerte derfor hendelse av type '{}'", fagsak.getSaksnummer().getVerdi(), payload.getHendelseType());
            }

        }
        return kandidaterTilRevurdering;
    }

    private Person hentPersonInformasjon(String ident) {
        var query = new HentPersonQueryRequest();
        query.setIdent(ident);
        var projection = new PersonResponseProjection()
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsRolle()
                .relatertPersonsIdent().minRolleForPerson());
        return pdlKlient.hentPerson(query, projection);
    }

    private LocalDate finnAktuellDato(Person personFraPdl) {
        return personFraPdl.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
    }

}

