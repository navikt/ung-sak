package no.nav.ung.sak.ytelse.ung.mottak;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Collection;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_SOKNAD_KODE)
public class DokumentMottakerSøknadUng implements Dokumentmottaker {

    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private FagsakRepository fagsakRepository;
    private UngdomsytelseSøknadPersisterer ungdomsytelseSøknadPersisterer;

    public DokumentMottakerSøknadUng() {
    }

    @Inject
    public DokumentMottakerSøknadUng(SøknadParser søknadParser, MottatteDokumentRepository mottatteDokumentRepository, FagsakRepository fagsakRepository, UngdomsytelseSøknadPersisterer ungdomsytelseSøknadPersisterer) {
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.fagsakRepository = fagsakRepository;
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
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);

            Ungdomsytelse ytelse = søknad.getYtelse();
            ungdomsytelseSøknadPersisterer.lagreSøknadEntitet(søknad, dokument.getJournalpostId(), behandlingId, Optional.of(ytelse.getSøknadsperiode()), dokument.getMottattDato());
            ungdomsytelseSøknadPersisterer.lagreSøknadsperioder(ytelse.getSøknadsperiodeList(), dokument.getJournalpostId(), behandlingId);
            ungdomsytelseSøknadPersisterer.oppdaterFagsakperiode(Optional.of(ytelse.getSøknadsperiode()), behandling.getFagsakId());
        }
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

}
