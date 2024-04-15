package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.ettersendelse.EttersendelseValidator;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.ETTERSENDELSE_PLEIEPENGER_BARN_KODE)
@DokumentGruppeRef(Brevkode.ETTERSENDELSE_PLEIEPENGER_LIVETS_SLUTTFASE_KODE)
public class EttersendelseDokumentValidator implements DokumentValidator {

    @Override
    public void validerDokumenter(Long behandlingId, Collection<MottattDokument> mottatteDokumenter) {
        for (MottattDokument mottattDokument : mottatteDokumenter) {
            validerDokument(mottattDokument);
        }
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        Objects.requireNonNull(mottattDokument);

        var ettersendelse = EttersendelseParser.parseDokument(mottattDokument);

        new EttersendelseValidator().forsikreValidert(ettersendelse);
    }
}
