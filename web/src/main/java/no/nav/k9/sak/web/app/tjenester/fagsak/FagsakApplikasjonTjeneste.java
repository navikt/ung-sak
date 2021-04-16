package no.nav.k9.sak.web.app.tjenester.fagsak;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private FagsakRepository fagsakRespository;

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
                                     TpsTjeneste tpsTjeneste, PersoninfoAdapter personinfoAdapter) {
        this.fagsakRespository = repositoryProvider.getFagsakRepository();
        this.tpsTjeneste = tpsTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    public Optional<PersoninfoBasis> hentBruker(Saksnummer saksnummer) {
        Optional<Fagsak> fagsak = fagsakRespository.hentSakGittSaksnummer(saksnummer);
        return fagsak.map(Fagsak::getAktørId).flatMap(personinfoAdapter::hentBrukerBasisForAktør);
    }

    public Optional<AsyncPollingStatus> sjekkProsessTaskPågår(Saksnummer saksnummer, String gruppe) {

        Optional<Fagsak> fagsak = fagsakRespository.hentSakGittSaksnummer(saksnummer);
        if (fagsak.isPresent()) {
            Long fagsakId = fagsak.get().getId();
            Map<String, ProsessTaskData> nesteTask = prosesseringAsynkTjeneste.sjekkProsessTaskPågår(fagsakId, null, gruppe);
            return new VurderProsessTaskStatusForPollingApi(FEIL, fagsakId).sjekkStatusNesteProsessTask(gruppe, nesteTask);
        } else {
            return Optional.empty();
        }

    }

    public List<FagsakInfoDto> matchFagsaker(FagsakYtelseType ytelseType,
                                             PersonIdent bruker,
                                             Periode periode,
                                             List<PersonIdent> pleietrengendeIdenter,
                                             List<PersonIdent> relatertAnnenPartIdenter) {
        var fom = periode == null ? null : periode.getFom();
        var tom = periode == null ? null : periode.getTom();

        AktørId brukerAktørId = finnAktørId(bruker);

        var fagsaker = fagsakRespository.finnFagsakRelatertTil(ytelseType, brukerAktørId, null, null, fom, tom);
        if (fagsaker.isEmpty()) {
            return Collections.emptyList();
        }

        class MatchIdenter {
            private final Map<AktørId, PersonIdent> mapPleietrengende;
            private final Map<AktørId, PersonIdent> mapRelatertAnnenPart;
            {
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
                    map.put(finnAktørId(ident), ident);
                }
            }

            boolean matcher(Fagsak f) {
                boolean match = true;
                if (f.getPleietrengendeAktørId() != null && !mapPleietrengende.isEmpty()) {
                    match &= mapPleietrengende.containsKey(f.getPleietrengendeAktørId());
                }
                if (match && f.getRelatertPersonAktørId() != null && !mapRelatertAnnenPart.isEmpty()) {
                    match &= mapRelatertAnnenPart.containsKey(f.getRelatertPersonAktørId());
                }
                return match;
            }

            PersonIdent getPleietrengende(AktørId aktørId) {
                var v = mapPleietrengende.get(aktørId);
                if (v == null && aktørId != null) {
                    v = personinfoAdapter.hentIdentForAktørId(aktørId).orElse(null);
                    mapPleietrengende.put(aktørId, v);
                }
                return v;
            }

            PersonIdent getRelatertAnnenPart(AktørId aktørId) {
                var v = mapRelatertAnnenPart.get(aktørId);
                if (v == null && aktørId != null) {
                    v = personinfoAdapter.hentIdentForAktørId(aktørId).orElse(null);
                    mapRelatertAnnenPart.put(aktørId, v);
                }
                return v;
            }

        }
        var identMap = new MatchIdenter();
        return fagsaker.stream().filter(f -> identMap.matcher(f))
            .map(f -> {
                return new FagsakInfoDto(f.getSaksnummer(),
                    f.getYtelseType(),
                    f.getStatus(),
                    new Periode(f.getPeriode().getFomDato(), f.getPeriode().getTomDato()),
                    bruker,
                    identMap.getPleietrengende(f.getPleietrengendeAktørId()),
                    identMap.getRelatertAnnenPart(f.getRelatertPersonAktørId()),
                    f.getSkalTilInfotrygd());
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
        Optional<Fagsak> fagsak = fagsakRespository.hentSakGittSaksnummer(saksnummer);
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
        List<Fagsak> fagsaker = fagsakRespository.hentForBruker(funnetNavBruker.get().getAktørId());
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
