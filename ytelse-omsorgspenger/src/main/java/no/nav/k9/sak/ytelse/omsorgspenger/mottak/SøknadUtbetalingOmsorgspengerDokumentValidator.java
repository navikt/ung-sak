package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import static no.nav.vedtak.feil.LogLevel.WARN;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.SØKNAD_UTBETALING_OMS_KODE)
public class SøknadUtbetalingOmsorgspengerDokumentValidator implements DokumentValidator {

    private SøknadParser søknadParser;

    SøknadUtbetalingOmsorgspengerDokumentValidator() {
        //for CDI proxy
    }

    @Inject
    public SøknadUtbetalingOmsorgspengerDokumentValidator(SøknadParser søknadParser) {
        this.søknadParser = søknadParser;
    }

    @Override
    public void validerDokumenter(String behandlingId, Collection<MottattDokument> meldinger) {
        validerHarInnhold(meldinger);
        List<Søknad> søknader = søknadParser.parseSøknader(meldinger);
        for (Søknad søknad : søknader) {
            validerInnhold(søknad);
        }
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        validerDokumenter(null, Set.of(mottattDokument));
    }

    private void validerInnhold(Søknad søknad) {
        OmsorgspengerUtbetaling ytelse = søknad.getYtelse();
        defaultValidering(ytelse);
        sanityCheck(ytelse);
        validerIkkeImplementertFunksjonalitet(ytelse);
    }

    private void sanityCheck(OmsorgspengerUtbetaling ytelse) {
        //TODO sanity check kan vurderes flyttet inn i kontrakt
        if (ytelse.getFraværsperioder().size() > 365) {
            throw valideringsfeil("Antallet fraværeperioder er " + ytelse.getFraværsperioder().size() + ", det gir ikke mening.");
        }
    }

    private void validerIkkeImplementertFunksjonalitet(OmsorgspengerUtbetaling ytelse) {
        if (ytelse.getFosterbarn() != null && !ytelse.getFosterbarn().isEmpty()) {
            throw new IllegalArgumentException("Fosterbarn er ikke (enda?) støttet i løsningen");
        }
    }

    private void defaultValidering(OmsorgspengerUtbetaling ytelse) {
        List<no.nav.k9.søknad.felles.Feil> feil = ytelse.getValidator().valider(ytelse);
        if (!feil.isEmpty()) {
            // kaster DokumentValideringException pga håndtering i SaksbehandlingDokumentmottakTjeneste
            throw valideringsfeil(feil.stream()
                .map(f -> "kode=" + f.feilkode + " for " + f.felt + ": " + f.feilmelding)
                .reduce((a, b) -> a + "; " + b).orElseThrow());
        }
    }

    private static void validerHarInnhold(Collection<MottattDokument> dokumenter) {
        Set<JournalpostId> dokumenterUtenInnhold = dokumenter.stream()
            .filter(d -> !d.harPayload())
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());
        if (!dokumenterUtenInnhold.isEmpty()) {
            throw valideringsfeil("Mottok søknad uten innhold. Gjelder journalpostId=" + dokumenterUtenInnhold);
        }
    }

    private static VLException valideringsfeil(String tekst) {
        return SøknadUtbetalingOmsorgspengerValideringFeil.FACTORY.valideringsfeilSøknadUtbetalingOmsorgspenger(tekst).toException();
    }

    interface SøknadUtbetalingOmsorgspengerValideringFeil extends DeklarerteFeil {
        SøknadUtbetalingOmsorgspengerValideringFeil FACTORY = FeilFactory.create(SøknadUtbetalingOmsorgspengerValideringFeil.class);

        @TekniskFeil(feilkode = "FP-642746", feilmelding = "Feil i søknad om utbetaling av omsorgspenger: %s", logLevel = WARN, exceptionClass = DokumentValideringException.class)
        Feil valideringsfeilSøknadUtbetalingOmsorgspenger(String detaljer);
    }
}
