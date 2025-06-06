package no.nav.ung.domenetjenester.sak;

import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.handler.WrappedProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.mottak.dokumentmottak.UngdomsytelseSøknadMottaker;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;

import java.util.Optional;

@ApplicationScoped
@ProsessTask(value = FinnEllerOpprettUngSakTask.TASKTYPE, maxFailedRuns = 1)
public class FinnEllerOpprettUngSakTask extends WrappedProsessTaskHandler {

    public static final String TASKTYPE = "ung.finnEllerOpprettSak";
    private static final Logger log = LoggerFactory.getLogger(FinnEllerOpprettUngSakTask.class);
    private final UngdomsytelseSøknadMottaker søknadMottakTjenester;

    @Inject
    public FinnEllerOpprettUngSakTask(FordelProsessTaskTjeneste fordelProsessTaskTjeneste,
                                      @FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE) UngdomsytelseSøknadMottaker søknadMottakTjenester) {
        super(fordelProsessTaskTjeneste);
        this.søknadMottakTjenester = søknadMottakTjenester;
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

        dataWrapper.getYtelseType()
            .map(it -> FagsakYtelseType.fraKode(it.getKode()))
            .filter(ytelseType -> ytelseType == FagsakYtelseType.UNGDOMSYTELSE)
            .orElseThrow(() -> new IllegalStateException("Støtter kun ungdomsytelser."));
    }

    @Override
    public MottattMelding doTask(MottattMelding dataWrapper) {
        var ytelseType = dataWrapper.getYtelseType().orElse(FagsakYtelseType.UNGDOMSYTELSE);

        var innsendtPeriode = getPeriode(dataWrapper);
        håndterStrukturertMelding(dataWrapper, ytelseType, innsendtPeriode);

        return dataWrapper.nesteSteg(UngEndeligJournalføringTask.TASKTYPE);
    }


    void håndterStrukturertMelding(MottattMelding dataWrapper, FagsakYtelseType ytelseType, Optional<Periode> innsendtPeriode) {
        Fagsak fagsak;
        if (innsendtPeriode.isEmpty()) {
            // TODO: Må sende med periode dersom vi skal støtte flere fagsaker for samme aktør
            fagsak = søknadMottakTjenester.finnEksisterendeFagsak(ytelseType, new AktørId(dataWrapper.getAktørId().orElseThrow()));
        } else {
            fagsak = søknadMottakTjenester.finnEllerOpprettFagsak(ytelseType, new AktørId(dataWrapper.getAktørId().orElseThrow()), innsendtPeriode.get().getFom(), innsendtPeriode.get().getTom());
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
