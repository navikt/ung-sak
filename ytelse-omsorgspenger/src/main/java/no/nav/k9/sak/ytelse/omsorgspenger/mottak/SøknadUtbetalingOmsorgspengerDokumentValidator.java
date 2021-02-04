package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

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
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.SØKNAD_UTBETALING_OMS_KODE)
public class SøknadUtbetalingOmsorgspengerDokumentValidator implements DokumentValidator {

    private SøknadParser søknadParser;
    private Boolean lansert;

    SøknadUtbetalingOmsorgspengerDokumentValidator() {
        //for CDI proxy
    }

    @Inject
    public SøknadUtbetalingOmsorgspengerDokumentValidator(SøknadParser søknadParser, @KonfigVerdi(value = "MOTTAK_SOKNAD_UTBETALING_OMS", defaultVerdi = "true") Boolean lansert) {
        this.søknadParser = søknadParser;
        this.lansert = lansert;
    }

    @Override
    public void validerDokumenter(String behandlingId, Collection<MottattDokument> meldinger) {
        validerLansert();
        validerHarInnhold(meldinger);
        var søknader = søknadParser.parseSøknader(meldinger);
        for (Søknad søknad : søknader) {
            validerInnhold(søknad);
        }
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        validerLansert();
        validerDokumenter(null, Set.of(mottattDokument));
    }

    private void validerLansert() {
        if (!lansert) {
            throw new IllegalArgumentException("Funksjonalitet for å ta i mot søknad om utbetaling av omsorgspenger er ikke lansert i dette miljøet");
        }
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
            throw new IllegalArgumentException("Fosterbarn er ikke støttet i løsningen");
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

    private static DokumentValideringException valideringsfeil(String tekst) {
        return new DokumentValideringException("Feil i søknad om utbetaling av omsorgspenger: " + tekst);
    }

}
