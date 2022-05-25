package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(Brevkode.INNTEKTSMELDING_KODE)
public class DokumentmottakerInntektsmelding implements Dokumentmottaker {

    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private DokumentmottakerFelles dokumentMottakerFelles;
    private boolean skruPåDokumentmottakPPN;

    DokumentmottakerInntektsmelding() {
        // for CDI
    }

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentMottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           @KonfigVerdi(value = "ENABLE_DOKUMENTMOTTAK_PPN", defaultVerdi = "true") boolean skruPåDokumentmottakPPN) {
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.skruPåDokumentmottakPPN = skruPåDokumentmottakPPN;
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        if (!skruPåDokumentmottakPPN && behandling.getFagsakYtelseType() == FagsakYtelseType.PPN){
            throw new IllegalArgumentException("Dokumentmottak for PPN er ikke lansert");
        }

        mottatteDokumentTjeneste.persisterInntektsmeldingForBehandling(behandling, mottattDokument);
        mottattDokument.forEach(m -> dokumentMottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), m.getJournalpostId(), m.getType()));
    }
}
