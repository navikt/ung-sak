package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.mottak;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.mottak.SøknadParser;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.Ytelse;
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerUtvidetRett;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@DokumentGruppeRef(Brevkode.SØKNAD_OMS_UTVIDETRETT_KS_KODE)
@DokumentGruppeRef(Brevkode.SØKNAD_OMS_UTVIDETRETT_MA_KODE)
public class SøknadDokumentValidator implements DokumentValidator {

    private static final Map<Ytelse.Type, Brevkode> GYLDIGE_SØKNAD_BREVKODER = Map.of(
        Ytelse.Type.OMSORGSPENGER_UTVIDETRETT_KRONISK_SYKT_BARN, Brevkode.SØKNAD_OMS_UTVIDETRETT_KS,
        Ytelse.Type.OMSORGSPENGER_UTVIDETRETT_MIDLERTIDIG_ALENE, Brevkode.SØKNAD_OMS_UTVIDETRETT_MA);

    @Inject
    public SøknadDokumentValidator() {
    }

    @Override
    public void validerDokumenter(Long behandlingId, Collection<MottattDokument> meldinger) {
        validerHarInnhold(meldinger);
        var mottattBrevkoder = meldinger.stream().map(MottattDokument::getType).collect(Collectors.toList());
        var søknader = new SøknadParser().parseSøknader(meldinger);

        int i = 0;
        for (Søknad søknad : søknader) {
            var brevkode = mottattBrevkoder.get(i++);
            Brevkode forventetBrevkode = GYLDIGE_SØKNAD_BREVKODER.get(søknad.getYtelse().getType());
            if (!Objects.equals(brevkode, forventetBrevkode)) {
                throw new IllegalArgumentException("Forventet brevkode: " + forventetBrevkode + ", fikk: " + brevkode);
            }
            validerInnhold(søknad);
        }
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        validerDokumenter(null, Set.of(mottattDokument));
    }

    private void validerInnhold(Søknad søknad) {
        OmsorgspengerUtvidetRett ytelse = søknad.getYtelse();
        defaultValidering(ytelse);
    }

    private void defaultValidering(OmsorgspengerUtvidetRett ytelse) {
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
