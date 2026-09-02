package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Aktivitetspenger;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.mottak.dokumentmottak.*;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerSøknadsperiodeTjeneste;

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
    private VirkingstidspunktUtleder virkingstidspunktUtleder;

    public AktivitetspengerSøknadDokumentMottaker() {
        //for CDI proxy
    }

    @Inject
    public AktivitetspengerSøknadDokumentMottaker(SøknadParser søknadParser, MottatteDokumentRepository mottatteDokumentRepository, AktivitetspengerSøknadPersisterer søknadPersisterer, HistorikkinnslagTjeneste historikkinnslagTjeneste, VirkingstidspunktUtleder virkingstidspunktUtleder) {
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadPersisterer = søknadPersisterer;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.virkingstidspunktUtleder = virkingstidspunktUtleder;
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

            søknadPersisterer.lagreSøknadEntitet(søknad, dokument.getJournalpostId(), behandlingId, startdato, dokument.getMottattDato());
            LocalDate virkningstidspunkt = virkingstidspunktUtleder.utledVirkingstidspunkt(ytelse.getSøknadsperiodeFom(), behandling.getId());
            LocalDateTimeline<Boolean> tidslinjeFraVirkningstidspunkt = AktivitetspengerSøknadsperiodeTjeneste.tidslinjeFraVirkningstidspunkt(virkningstidspunkt);
            søknadPersisterer.lagreVirkningsdato(virkningstidspunkt, dokument.getJournalpostId(), dokument.getMottattTidspunkt(), behandlingId, ytelse.getErBosattITrondheim());
            søknadPersisterer.oppdaterFagsakperiode(new Periode(tidslinjeFraVirkningstidspunkt.getMinLocalDate(), tidslinjeFraVirkningstidspunkt.getMaxLocalDate()), behandling);
            søknadPersisterer.lagreForutgåendeMedlemskapGrunnlag(ytelse.getForutgåendeBosteder(), ytelse.getSøknadsperiode(), dokument.getJournalpostId(), behandlingId);

            historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), behandlingId, dokument.getJournalpostId());
        }
        mottatteDokumentRepository.oppdaterStatus(mottattDokument.stream().toList(), DokumentStatus.GYLDIG);
    }

    @Override
    public List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument) {
        return mottattDokument.stream().map(it -> søknadParser.parseSøknad(it))
            .map(it -> it.getYtelse().getSøknadsperiode())
            .map(it -> new Trigger(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFraOgMed(), it.getTilOgMed()), BehandlingÅrsakType.NY_SØKT_PERIODE))
            .toList();
    }

}
