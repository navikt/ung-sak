package no.nav.k9.sak.web.app.tjenester.fagsak;

import java.time.LocalDate;
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

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.fagsak.FagsakInfoDto;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.VurderProsessTaskStatusForPollingApi;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

@ApplicationScoped
public class FagsakApplikasjonTjeneste {
    private static FagsakProsessTaskFeil FEIL = FeilFactory.create(FagsakProsessTaskFeil.class);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> vurderSøknadsfristTjeneste;

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
                                     PersoninfoAdapter personinfoAdapter,
                                     BehandlingRepository behandlingRepository,
                                     @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> vurderSøknadsfristTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.tpsTjeneste = tpsTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = behandlingRepository;
        this.vurderSøknadsfristTjeneste = vurderSøknadsfristTjeneste;
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
                    f.getSkalTilInfotrygd(),
                    Collections.emptyList());
            })
            .collect(Collectors.toList());

    }

    public FagsakInfoDto hentSøknadsperioder(FagsakYtelseType ytelseType, PersonIdent personIdent, List<PersonIdent> pleietrengendeIdenter) {
        AktørId aktørId = finnAktørId(personIdent);
        if ((pleietrengendeIdenter.size() != 1)) {
            throw new IllegalStateException("Søtter bare ett barn");
        }
        AktørId pleietrengendeAktør = finnAktørId(pleietrengendeIdenter.get(0));
        var fagsaker = fagsakRepository.finnFagsakRelatertTilEnAvAktører(ytelseType, aktørId, List.of(pleietrengendeAktør), Collections.emptyList(), null, null);
        Optional<LocalDate> max = fagsaker.stream().map(fagsak1 -> fagsak1.getPeriode().getFomDato()).max(LocalDate::compareTo);
        if (max.isPresent()) {
            Optional<Fagsak> fagsakOptional = fagsaker.stream().collect(Collectors.groupingBy(fagsak -> fagsak.getPeriode().getFomDato())).get(max.get()).stream().findFirst();
            if (fagsakOptional.isPresent()) {
                Fagsak fagsak = fagsakOptional.get();
                Optional<Behandling> behandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId());
                if (behandling.isPresent()) {
                    List<LocalDateSegment<Boolean>> segmenter = vurderSøknadsfristTjeneste.hentPerioderTilVurdering(BehandlingReferanse.fra(behandling.get())).values().stream().flatMap(p -> p.stream().map(SøktPeriode::getPeriode)).map(l -> new LocalDateSegment<>(l.toLocalDateInterval(), true)).collect(Collectors.toList());
                    LocalDateTimeline<Boolean> tidslinje = new LocalDateTimeline<>(segmenter);
                    LocalDateTimeline<Boolean> compress = tidslinje.compress();
                    List<Periode> søknadsperioder = compress.toSegments().stream().map(s -> new Periode(s.getFom(), s.getTom())).collect(Collectors.toList());

                   return new FagsakInfoDto(
                        fagsak.getSaksnummer(),
                        ytelseType,
                        fagsak.getStatus(),
                        new Periode(fagsak.getPeriode().getFomDato(), fagsak.getPeriode().getTomDato()),
                        personinfoAdapter.hentIdentForAktørId(fagsak.getAktørId()).orElseThrow(() -> new IllegalArgumentException("Finner ikke personIdent for bruker")),
                        null,
                        null,
                        fagsak.getSkalTilInfotrygd(),
                        søknadsperioder);
                }
            }
        }
        return null;
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
