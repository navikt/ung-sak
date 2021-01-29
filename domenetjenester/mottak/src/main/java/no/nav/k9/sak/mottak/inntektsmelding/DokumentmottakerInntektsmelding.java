package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;

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
    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    @Override
    public void mottaDokument(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        mottatteDokumentTjeneste.persisterInntektsmeldingForBehandling(behandling, mottattDokument);
        mottattDokument.forEach(m -> dokumentMottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), m.getJournalpostId(), m.getType()));
    }
}
