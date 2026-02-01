package no.nav.ung.domenetjenester.sak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.handler.WrappedProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.mottak.SøknadMottakTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
@ProsessTask(value = FinnEllerOpprettUngSakTask.TASKTYPE, maxFailedRuns = 1)
public class FinnEllerOpprettUngSakTask extends WrappedProsessTaskHandler {

    public static final String TASKTYPE = "ung.finnEllerOpprettSak";
    private static final Logger log = LoggerFactory.getLogger(FinnEllerOpprettUngSakTask.class);
    private final Map<FagsakYtelseType, SøknadMottakTjeneste> søknadMottakTjenester;
    private final boolean aktivitetspengerEnabled;

    @Inject
    public FinnEllerOpprettUngSakTask(FordelProsessTaskTjeneste fordelProsessTaskTjeneste,
                                      @Any Instance<SøknadMottakTjeneste> søknadMottakTjenester,
                                      @KonfigVerdi(value = "aktivitetspenger.enabled", required = false, defaultVerdi = "false") boolean aktivitetspengerEnabled) {
        super(fordelProsessTaskTjeneste);
        this.aktivitetspengerEnabled = aktivitetspengerEnabled;
        this.søknadMottakTjenester = new EnumMap<>(FagsakYtelseType.class);
        this.søknadMottakTjenester.put(FagsakYtelseType.UNGDOMSYTELSE, SøknadMottakTjeneste.finnTjeneste(søknadMottakTjenester, FagsakYtelseType.UNGDOMSYTELSE));
        this.søknadMottakTjenester.put(FagsakYtelseType.AKTIVITETSPENGER, SøknadMottakTjeneste.finnTjeneste(søknadMottakTjenester, FagsakYtelseType.AKTIVITETSPENGER));
    }

    @Override
    public void precondition(MottattMelding dataWrapper) {
        if (dataWrapper.getAktørId().isEmpty()) {
            throw new IllegalStateException("Mangler påkrevd aktørId.");
        }

        var manglerYtelseType = dataWrapper.getYtelseType().isEmpty();
        if (dataWrapper.getBehandlingTema() == null && manglerYtelseType) {
            throw new IllegalStateException("Mangler påkrevd behandlingTema eller ytelseType.");
        }
        if (aktivitetspengerEnabled){
            //TODO kan vi kreve at ytelsetype alltid er satt?
            //så lenge vi har samme samme behandlingstema for begge ytelsestyper, må vi kreve ytelsestype her
            if (manglerYtelseType){
                throw new IllegalArgumentException("Mangler ytelsestype. Behandling tema=" +dataWrapper.getBehandlingTema());
            }
            Set<FagsakYtelseType> støttedeYtelseTyper = Set.of(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);
            dataWrapper.getYtelseType()
                .map(it -> FagsakYtelseType.fraKode(it.getKode()))
                .filter(støttedeYtelseTyper::contains)
                .orElseThrow(() -> new IllegalStateException("Støtter kun ungdomsytelser."));
        } else {
            dataWrapper.getYtelseType()
                .map(it -> FagsakYtelseType.fraKode(it.getKode()))
                .filter(FagsakYtelseType.UNGDOMSYTELSE::equals)
                .orElseThrow(() -> new IllegalStateException("Støtter kun ungdomsytelser."));
        }

    }

    @Override
    public MottattMelding doTask(MottattMelding dataWrapper) {
        var ytelseType = dataWrapper.getYtelseType().orElse(FagsakYtelseType.UNGDOMSYTELSE);

        var innsendtPeriode = getPeriode(dataWrapper);
        håndterStrukturertMelding(dataWrapper, ytelseType, innsendtPeriode);

        return dataWrapper.nesteSteg(UngEndeligJournalføringTask.TASKTYPE);
    }


    void håndterStrukturertMelding(MottattMelding dataWrapper, FagsakYtelseType ytelseType, Optional<Periode> innsendtPeriode) {
        SøknadMottakTjeneste søknadMottakTjeneste = søknadMottakTjenester.get(ytelseType);
        Fagsak fagsak;
        if (innsendtPeriode.isEmpty()) {
            // TODO: Må sende med periode dersom vi skal støtte flere fagsaker for samme aktør
            fagsak = søknadMottakTjeneste.finnEksisterendeFagsak(ytelseType, new AktørId(dataWrapper.getAktørId().orElseThrow()));
        } else {
            fagsak = søknadMottakTjeneste.finnEllerOpprettFagsak(ytelseType, new AktørId(dataWrapper.getAktørId().orElseThrow()), innsendtPeriode.get().getFom(), innsendtPeriode.get().getTom());
        }
        log.info("Fant eller opprettet sak for ytelse='{}' med saksnummer='{}' for innsendt periode={}", ytelseType, fagsak.getSaksnummer(), innsendtPeriode);
        dataWrapper.setSaksnummer(fagsak.getSaksnummer().getVerdi());
    }

    private Optional<Periode> getPeriode(MottattMelding dataWrapper) {
        var fom = dataWrapper.getFørsteUttaksdag();
        if (fom.isEmpty()) {
            return Optional.empty();
        }
        var tom = dataWrapper.getSisteUttaksdag().orElse(null /* vil avkortes i ungsak om nødvendig */);
        return Optional.of(new Periode(fom.get(), tom));
    }

    @Override
    public void postcondition(MottattMelding dataWrapper) {
        if (dataWrapper.getSaksnummer().isEmpty()) {
            throw new IllegalStateException("Mangler påkrevd saksnummer.");
        }
    }
}
