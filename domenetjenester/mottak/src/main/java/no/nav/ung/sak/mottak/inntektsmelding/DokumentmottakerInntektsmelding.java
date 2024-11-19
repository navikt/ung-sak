package no.nav.ung.sak.mottak.inntektsmelding;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.ung.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(Brevkode.INNTEKTSMELDING_KODE)
public class DokumentmottakerInntektsmelding implements Dokumentmottaker {

    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private DokumentmottakerFelles dokumentMottakerFelles;

    DokumentmottakerInntektsmelding() {
        // for CDI
    }

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentMottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        mottatteDokumentTjeneste.persisterInntektsmeldingForBehandling(behandling, mottattDokument);
        mottattDokument.forEach(m -> dokumentMottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), m.getJournalpostId(), m.getType()));
    }
}
