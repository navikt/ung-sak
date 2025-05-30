package no.nav.ung.sak.web.app.tjenester.fagsak;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.felles.feil.FeilFactory;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.kontrakt.AsyncPollingStatus;
import no.nav.ung.sak.kontrakt.fagsak.FagsakInfoDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.VurderProsessTaskStatusForPollingApi;

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
     * Dette er lagt på som begrensning, slik at all tilgangskontroll håndteres før søker utføres (caller må kjenne til alle personer han ønsker
     * å søke etter).
     */
    public List<FagsakInfoDto> matchFagsaker(FagsakYtelseType ytelseType,
                                             PersonIdent bruker,
                                             Periode periode) {

        var fom = periode == null ? null : periode.getFom();
        var tom = periode == null ? null : periode.getTom();

        AktørId brukerAktørId = finnAktørId(bruker);


        var fagsaker = fagsakRepository.finnFagsakRelatertTil(ytelseType, brukerAktørId, fom, tom);
        return fagsaker.stream()
            .map(f -> new FagsakInfoDto(f.getSaksnummer(),
                f.getYtelseType(),
                f.getStatus(),
                new Periode(f.getPeriode().getFomDato(), f.getPeriode().getTomDato()),
                personinfoAdapter.hentIdentForAktørId(f.getAktørId()).orElseThrow(() -> new IllegalArgumentException("Finner ikke personIdent for bruker"))
            ))
            .collect(Collectors.toList());

    }

    public FagsakSamlingForBruker hentSaker(String søkestreng) {
        if (predikatErFnr.test(søkestreng)) {
            return hentSakerForFnr(new PersonIdent(søkestreng));
        } else {
            return hentFagsakForSaksnummer(new Saksnummer(søkestreng));
        }
    }

    /**
     * Returnerer samling med kun en fagsak.
     */
    public FagsakSamlingForBruker hentFagsakForSaksnummer(Saksnummer saksnummer) {
        Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (fagsak.isEmpty()) {
            return FagsakSamlingForBruker.emptyView();
        }
        List<Fagsak> fagsaker = Collections.singletonList(fagsak.get());
        AktørId aktørId = fagsak.get().getAktørId();

        Optional<Personinfo> funnetNavBruker = tpsTjeneste.hentBrukerForAktør(aktørId);
        if (funnetNavBruker.isEmpty()) {
            return FagsakSamlingForBruker.emptyView();
        }

        return tilFagsakView(fagsaker, funnetNavBruker.get());
    }

    private FagsakSamlingForBruker hentSakerForFnr(PersonIdent fnr) {
        Optional<Personinfo> funnetNavBruker = tpsTjeneste.hentBrukerForFnr(fnr);
        if (funnetNavBruker.isEmpty()) {
            return FagsakSamlingForBruker.emptyView();
        }
        List<Fagsak> fagsaker = fagsakRepository.hentForBruker(funnetNavBruker.get().getAktørId());
        return tilFagsakView(fagsaker, funnetNavBruker.get());
    }

    private FagsakSamlingForBruker tilFagsakView(List<Fagsak> fagsaker, Personinfo personinfo) {
        FagsakSamlingForBruker view = new FagsakSamlingForBruker(personinfo);
        // FIXME K9 relevante data
        fagsaker.forEach(sak -> view.leggTil(sak));
        return view;
    }

    private AktørId finnAktørId(PersonIdent bruker) {
        if (bruker == null)
            return null;
        return bruker.erAktørId()
            ? new AktørId(bruker.getAktørId())
            : personinfoAdapter.hentAktørIdForPersonIdent(bruker).orElseThrow(() -> new IllegalArgumentException("Finner ikke aktørId for bruker"));
    }

}
