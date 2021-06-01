package no.nav.k9.sak.web.app.tjenester.fagsak;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.fagsak.FagsakInfoDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.VurderProsessTaskStatusForPollingApi;

@ApplicationScoped
public class FagsakApplikasjonTjeneste {
    private static FagsakProsessTaskFeil FEIL = FeilFactory.create(FagsakProsessTaskFeil.class);

    private FagsakRepository fagsakRepository;

    private TpsTjeneste tpsTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    private Predicate<String> predikatErFnr = søkestreng -> søkestreng.matches("\\d{11}");

    protected FagsakApplikasjonTjeneste() {
        //
    }

    @Inject
    public FagsakApplikasjonTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                     TpsTjeneste tpsTjeneste,
                                     PersoninfoAdapter personinfoAdapter
                                     ) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.tpsTjeneste = tpsTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    public Optional<PersoninfoBasis> hentBruker(Saksnummer saksnummer) {
        Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        return fagsak.map(Fagsak::getAktørId).flatMap(personinfoAdapter::hentBrukerBasisForAktør);
    }

    public Optional<AsyncPollingStatus> sjekkProsessTaskPågår(Saksnummer saksnummer, String gruppe) {

        Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (fagsak.isPresent()) {
            Long fagsakId = fagsak.get().getId();
            Map<String, ProsessTaskData> nesteTask = prosesseringAsynkTjeneste.sjekkProsessTaskPågår(fagsakId, null, gruppe);
            return new VurderProsessTaskStatusForPollingApi(FEIL, fagsakId).sjekkStatusNesteProsessTask(gruppe, nesteTask);
        } else {
            return Optional.empty();
        }

    }

    /**
     * Finner matchende fagsaker for ytelse+bruker+periode .
     * Hvis pleietrengende/relatertAnnenPart oppgis matches disse også. Merk dersom fagsak har pleietrengende/relatertAnnenPart OG det ikke
     * oppgis som input, vil ikke fagsaken matche.
     * Dette er lagt på som begrensning, slik at all tilgangskontroll håndteres før søker utføres (caller må kjenne til alle personer han ønsker
     * å søke etter).
     */
    public List<FagsakInfoDto> matchFagsaker(FagsakYtelseType ytelseType,
                                             PersonIdent bruker,
                                             Periode periode,
                                             List<PersonIdent> pleietrengendeIdenter,
                                             List<PersonIdent> relatertAnnenPartIdenter) {

        class MatchIdenter {
            private final Map<AktørId, PersonIdent> mapPleietrengende;
            private final Map<AktørId, PersonIdent> mapRelatertAnnenPart;

            MatchIdenter(List<PersonIdent> pleietrengendeIdenter, List<PersonIdent> relatertAnnenPartIdenter) {
                this.mapPleietrengende = new LinkedHashMap<>();
                this.mapRelatertAnnenPart = new LinkedHashMap<>();
                if (pleietrengendeIdenter != null) {
                    pleietrengendeIdenter.forEach(id -> leggTil(mapPleietrengende, id));
                }
                if (relatertAnnenPartIdenter != null) {
                    relatertAnnenPartIdenter.forEach(id -> leggTil(mapRelatertAnnenPart, id));
                }
            }

            private void leggTil(Map<AktørId, PersonIdent> map, PersonIdent ident) {
                if (ident.erAktørId()) {
                    map.put(new AktørId(ident.getAktørId()), ident);
                } else if (ident.erNorskIdent()) {
                    AktørId aktørId = finnAktørId(ident);
                    map.put(aktørId, ident);
                }
            }

            Map<AktørId, PersonIdent> getPleietrengende() {
                return Collections.unmodifiableMap(mapPleietrengende);
            }

            Map<AktørId, PersonIdent> getRelatertAnnenPart() {
                return Collections.unmodifiableMap(mapRelatertAnnenPart);
            }

        }
        var identMap = new MatchIdenter(pleietrengendeIdenter, relatertAnnenPartIdenter);

        var fom = periode == null ? null : periode.getFom();
        var tom = periode == null ? null : periode.getTom();

        AktørId brukerAktørId = finnAktørId(bruker);

        Set<AktørId> pleietrengendeAktørIder = identMap.getPleietrengende().keySet();
        Set<AktørId> relatertAnnenPartAktørIder = identMap.getRelatertAnnenPart().keySet();

        var fagsaker = fagsakRepository.finnFagsakRelatertTilEnAvAktører(ytelseType, brukerAktørId, pleietrengendeAktørIder, relatertAnnenPartAktørIder, fom, tom);
        return fagsaker.stream()
            .map(f -> {
                return new FagsakInfoDto(f.getSaksnummer(),
                    f.getYtelseType(),
                    f.getStatus(),
                    new Periode(f.getPeriode().getFomDato(), f.getPeriode().getTomDato()),
                    personinfoAdapter.hentIdentForAktørId(f.getAktørId()).orElseThrow(() -> new IllegalArgumentException("Finner ikke personIdent for bruker")),
                    identMap.getPleietrengende().get(f.getPleietrengendeAktørId()),
                    identMap.getRelatertAnnenPart().get(f.getRelatertPersonAktørId()),
                    f.getSkalTilInfotrygd()
                );
            })
            .collect(Collectors.toList());

    }

    public FagsakSamlingForBruker hentSaker(String søkestreng) {
        if (predikatErFnr.test(søkestreng)) {
            return hentSakerForFnr(new PersonIdent(søkestreng));
        } else {
            return hentFagsakForSaksnummer(new Saksnummer(søkestreng));
        }
    }

    /** Returnerer samling med kun en fagsak. */
    public FagsakSamlingForBruker hentFagsakForSaksnummer(Saksnummer saksnummer) {
        Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (!fagsak.isPresent()) {
            return FagsakSamlingForBruker.emptyView();
        }
        List<Fagsak> fagsaker = Collections.singletonList(fagsak.get());
        AktørId aktørId = fagsak.get().getAktørId();

        Optional<Personinfo> funnetNavBruker = tpsTjeneste.hentBrukerForAktør(aktørId);
        if (!funnetNavBruker.isPresent()) {
            return FagsakSamlingForBruker.emptyView();
        }

        return tilFagsakView(fagsaker, finnAntallBarnTps(fagsaker), funnetNavBruker.get());
    }

    private FagsakSamlingForBruker hentSakerForFnr(PersonIdent fnr) {
        Optional<Personinfo> funnetNavBruker = tpsTjeneste.hentBrukerForFnr(fnr);
        if (!funnetNavBruker.isPresent()) {
            return FagsakSamlingForBruker.emptyView();
        }
        List<Fagsak> fagsaker = fagsakRepository.hentForBruker(funnetNavBruker.get().getAktørId());
        return tilFagsakView(fagsaker, finnAntallBarnTps(fagsaker), funnetNavBruker.get());
    }

    private FagsakSamlingForBruker tilFagsakView(List<Fagsak> fagsaker, Map<Long, Integer> antallBarnPerFagsak, Personinfo personinfo) {
        FagsakSamlingForBruker view = new FagsakSamlingForBruker(personinfo);
        // FIXME K9 relevante data
        fagsaker.forEach(sak -> view.leggTil(sak, antallBarnPerFagsak.get(sak.getId()), null));
        return view;
    }

    private Map<Long, Integer> finnAntallBarnTps(List<Fagsak> fagsaker) {
        Map<Long, Integer> antallBarnPerFagsak = new HashMap<>();
        for (Fagsak fagsak : fagsaker) {
            antallBarnPerFagsak.put(fagsak.getId(), 0); // FIXME: Skal ikke være hardkodet.
        }
        return antallBarnPerFagsak;
    }

    private AktørId finnAktørId(PersonIdent bruker) {
        if (bruker == null)
            return null;
        return bruker.erAktørId()
            ? new AktørId(bruker.getAktørId())
            : personinfoAdapter.hentAktørIdForPersonIdent(bruker).orElseThrow(() -> new IllegalArgumentException("Finner ikke aktørId for bruker"));
    }

}
