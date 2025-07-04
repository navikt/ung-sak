package no.nav.ung.sak.mottak.dokumentmottak;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_SOKNAD_KODE)
public class DokumentMottakerSøknadUng implements Dokumentmottaker {

    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UngdomsytelseSøknadPersisterer ungdomsytelseSøknadPersisterer;

    public DokumentMottakerSøknadUng() {
    }

    @Inject
    public DokumentMottakerSøknadUng(SøknadParser søknadParser, MottatteDokumentRepository mottatteDokumentRepository, UngdomsytelseSøknadPersisterer ungdomsytelseSøknadPersisterer) {
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ungdomsytelseSøknadPersisterer = ungdomsytelseSøknadPersisterer;
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
            Ungdomsytelse ytelse = søknad.getYtelse();
            if (ytelse.getStartdatoer().size() != 1) {
                throw new IllegalStateException("Forventet at søknaden inneholder nøyaktig én startdato, fant " + ytelse.getStartdatoer().size() + " startdatoer.");
            }
            ungdomsytelseSøknadPersisterer.lagreSøknadEntitet(søknad, dokument.getJournalpostId(), behandlingId, ytelse.getStartdatoer().get(0), dokument.getMottattDato());
            ungdomsytelseSøknadPersisterer.lagreSøknadsperioder(ytelse.getStartdatoer(), dokument.getJournalpostId(), behandlingId);
            ungdomsytelseSøknadPersisterer.oppdaterFagsakperiode(ytelse.getSøknadsperiode().getFraOgMed(), behandling);

        }
        mottatteDokumentRepository.oppdaterStatus(mottattDokument.stream().toList(), DokumentStatus.GYLDIG);
    }

    @Override
    public List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument) {
        return mottattDokument.stream().map(it -> søknadParser.parseSøknad(it))
            .map(it -> ((Ungdomsytelse) it.getYtelse()).getSøknadsperiode())
            .map(it -> new Trigger(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFraOgMed(), it.getTilOgMed()), BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE))
            .toList();
    }

}
