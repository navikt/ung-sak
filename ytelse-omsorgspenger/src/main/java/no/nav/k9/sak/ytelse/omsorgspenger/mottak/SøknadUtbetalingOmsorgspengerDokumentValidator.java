package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.SØKNAD_UTBETALING_OMS_KODE)
@DokumentGruppeRef(Brevkode.SØKNAD_UTBETALING_OMS_AT_KODE)
@DokumentGruppeRef(Brevkode.FRAVÆRSKORRIGERING_IM_OMS_KODE)
public class SøknadUtbetalingOmsorgspengerDokumentValidator implements DokumentValidator {

    private SøknadParser søknadParser;

    SøknadUtbetalingOmsorgspengerDokumentValidator() {
        // for CDI proxy
    }

    @Inject
    public SøknadUtbetalingOmsorgspengerDokumentValidator(SøknadParser søknadParser) {
        this.søknadParser = søknadParser;
    }

    @Override
    public void validerDokumenter(Long behandlingId, Collection<MottattDokument> meldinger) {
        validerHarInnhold(meldinger);
        var mottattBrevkoder = meldinger.stream().map(MottattDokument::getType).collect(Collectors.toList());
        var søknader = søknadParser.parseSøknader(meldinger);

        int i = 0;
        for (Søknad søknad : søknader) {
            var brevkode = mottattBrevkoder.get(i++);
            List<Brevkode> forventetBrevkoder = List.of(Brevkode.SØKNAD_UTBETALING_OMS, Brevkode.SØKNAD_UTBETALING_OMS_AT, Brevkode.FRAVÆRSKORRIGERING_IM_OMS);
            if (!forventetBrevkoder.contains(brevkode)) {
                throw new IllegalArgumentException("Forventet brevkode: " + forventetBrevkoder + ", fikk: " + brevkode);
            }
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
    }

    private void sanityCheck(OmsorgspengerUtbetaling ytelse) {
        // TODO sanity check kan vurderes flyttet inn i kontrakt
        if (ytelse.getFraværsperioder().size() > 365) {
            throw valideringsfeil("Antallet fraværeperioder er " + ytelse.getFraværsperioder().size() + ", det gir ikke mening.");
        }
    }

    private void defaultValidering(OmsorgspengerUtbetaling ytelse) {
        List<no.nav.k9.søknad.felles.Feil> feil = ytelse.getValidator().valider(ytelse);
        if (!feil.isEmpty()) {
            // kaster DokumentValideringException pga håndtering i SaksbehandlingDokumentmottakTjeneste
            throw valideringsfeil(feil.stream()
                .map(f -> "kode=" + f.getFeilkode() + " for " + f.getFelt() + ": " + f.getFeilmelding())
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
