package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.ettersendelse.EttersendelseValidator;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.ETTERSENDELSE_PLEIEPENGER_BARN_KODE)
@DokumentGruppeRef(Brevkode.ETTERSENDELSE_PLEIEPENGER_LIVETS_SLUTTFASE_KODE)
public class EttersendelseDokumentValidator implements DokumentValidator {

    private boolean ettersendelseRettTilK9Sak;

    EttersendelseDokumentValidator() {
        // CDI
    }

    @Inject
    public EttersendelseDokumentValidator(@KonfigVerdi(value = "ETTERSENDELSE_RETT_TIL_K9SAK", defaultVerdi = "false") boolean ettersendelseRettTilK9Sak) {
        this.ettersendelseRettTilK9Sak = ettersendelseRettTilK9Sak;
    }

    @Override
    public void validerDokumenter(Long behandlingId, Collection<MottattDokument> mottatteDokumenter) {
        for (MottattDokument mottattDokument : mottatteDokumenter) {
            validerDokument(mottattDokument);
        }
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        Objects.requireNonNull(mottattDokument);

        if (!ettersendelseRettTilK9Sak) {
            throw new IllegalStateException("Funksjonaliteten er skrudd av");
        }

        var ettersendelse = EttersendelseParser.parseDokument(mottattDokument);

        new EttersendelseValidator().forsikreValidert(ettersendelse);
    }
}
