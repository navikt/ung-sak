package no.nav.k9.sak.web.app.tjenester.fagsak;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.VurderProsessTaskStatusForPollingApi;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
public class FagsakApplikasjonTjeneste {
    private static FagsakProsessTaskFeil FEIL = FeilFactory.create(FagsakProsessTaskFeil.class);

    private FagsakRepository fagsakRespository;

    private TpsTjeneste tpsTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    private Predicate<String> predikatErFnr = søkestreng -> søkestreng.matches("\\d{11}");

    protected FagsakApplikasjonTjeneste() {
        //CDI runner
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

    public FagsakSamlingForBruker hentSaker(String søkestreng) {
        if (predikatErFnr.test(søkestreng)) {
            return hentSakerForFnr(new PersonIdent(søkestreng));
        } else {
            return hentFagsakForSaksnummer(new Saksnummer(søkestreng));
        }
    }

    private FagsakSamlingForBruker hentSakerForFnr(PersonIdent fnr) {
        Optional<Personinfo> funnetNavBruker = tpsTjeneste.hentBrukerForFnr(fnr);
        if (!funnetNavBruker.isPresent()) {
            return FagsakSamlingForBruker.emptyView();
        }
        List<Fagsak> fagsaker = fagsakRespository.hentForBruker(funnetNavBruker.get().getAktørId());
        return tilFagsakView(fagsaker, finnAntallBarnTps(fagsaker), funnetNavBruker.get());
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

    public Optional<PersoninfoBasis> hentBruker(Saksnummer saksnummer) {
        Optional<Fagsak> fagsak = fagsakRespository.hentSakGittSaksnummer(saksnummer);
        return fagsak.map(Fagsak::getAktørId).flatMap(personinfoAdapter::hentBrukerBasisForAktør);
    }

}
