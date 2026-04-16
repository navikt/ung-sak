package no.nav.ung.ytelse.ungdomsprogramytelsen.mottak.inntektrapportering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.mottak.dokumentmottak.*;
import no.nav.ung.sak.mottak.dokumentmottak.inntektrapportering.InntektsrapporteringAsyncPersisterer;
import no.nav.ung.sak.typer.JournalpostId;

import java.util.Collection;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING_KODE)
public class DokumentMottakerInntektrapportering implements Dokumentmottaker {

    private SøknadParser søknadParser;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private InntektsrapporteringAsyncPersisterer asyncPersisterer;


    public DokumentMottakerInntektrapportering() {
    }

    @Inject
    public DokumentMottakerInntektrapportering(SøknadParser søknadParser,
                                               HistorikkinnslagTjeneste historikkinnslagTjeneste,
                                               InntektsrapporteringAsyncPersisterer asyncPersisterer) {
        this.søknadParser = søknadParser;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.asyncPersisterer = asyncPersisterer;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        var behandlingId = behandling.getId();
        for (MottattDokument dokument : mottattDokument) {
            var søknad = søknadParser.parseSøknad(dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            if (søknad.getKildesystem().isPresent()) {
                dokument.setKildesystem(søknad.getKildesystem().get().getKode());
            }
            opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), behandling.getId(), dokument.getJournalpostId());
            asyncPersisterer.opprettTaskForPersistering(behandling, dokument, ((Ungdomsytelse) søknad.getYtelse()).getInntekter());
        }
    }

    private void opprettHistorikkinnslagForVedlegg(Long fagsakId, Long behandlingId, JournalpostId journalpostId) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(fagsakId, behandlingId, journalpostId);
    }

    @Override
    public List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument) {
        return mottattDokument.stream().map(it -> søknadParser.parseSøknad(it))
            .map(it -> ((Ungdomsytelse) it.getYtelse()).getInntekter().getMinMaksPeriode())
            .map(it -> new Trigger(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFraOgMed(), it.getTilOgMed()), BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT))
            .toList();
    }

}
