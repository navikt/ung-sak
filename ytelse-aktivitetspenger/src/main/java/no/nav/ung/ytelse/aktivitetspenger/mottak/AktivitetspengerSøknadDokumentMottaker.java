package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Aktivitetspenger;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.ung.sak.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.ung.sak.mottak.dokumentmottak.Trigger;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;


@ApplicationScoped
@FagsakYtelseTypeRef(AKTIVITETSPENGER)
@DokumentGruppeRef(Brevkode.AKTIVITETSPENGER_SOKNAD_KODE)
public class AktivitetspengerSøknadDokumentMottaker implements Dokumentmottaker {

    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private AktivitetspengerSøknadPersisterer søknadPersisterer;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    public AktivitetspengerSøknadDokumentMottaker() {
    }

    @Inject
    public AktivitetspengerSøknadDokumentMottaker(SøknadParser søknadParser, MottatteDokumentRepository mottatteDokumentRepository, AktivitetspengerSøknadPersisterer søknadPersisterer, HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadPersisterer = søknadPersisterer;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
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
            Aktivitetspenger ytelse = søknad.getYtelse();
            LocalDate startdato = ytelse.getSøknadsperiode().getFraOgMed();
            //TODO mulig søknad entitet bør utvides med tom dersom det blir fom/tom i søknaden
            søknadPersisterer.lagreSøknadEntitet(søknad, dokument.getJournalpostId(), behandlingId, startdato, dokument.getMottattDato());
            søknadPersisterer.lagreSøknadsperioder(ytelse.getSøknadsperiode(), dokument.getJournalpostId(), dokument.getMottattTidspunkt(), behandlingId);
            søknadPersisterer.oppdaterFagsakperiode(new Periode(ytelse.getSøknadsperiode().getFraOgMed(), ytelse.getSøknadsperiode().getTilOgMed()), behandling);

            historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), behandlingId, dokument.getJournalpostId());
        }
        mottatteDokumentRepository.oppdaterStatus(mottattDokument.stream().toList(), DokumentStatus.GYLDIG);
    }

    @Override
    public List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument) {
        return mottattDokument.stream().map(it -> søknadParser.parseSøknad(it))
            .map(it -> it.getYtelse().getSøknadsperiode())
            .map(it -> new Trigger(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFraOgMed(), it.getTilOgMed()), BehandlingÅrsakType.NY_SØKT_AKTIVITETSPENGER_PERIODE))
            .toList();
    }

}
