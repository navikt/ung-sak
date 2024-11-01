package no.nav.k9.sak.ytelse.ung.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.søknad.felles.type.Periode;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_SOKNAD_KODE)
public class DokumentMottakerSøknadUng implements Dokumentmottaker {

    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private FagsakRepository fagsakRepository;
    private UngdomsytelseSøknadPersisterer ungdomsytelseSøknadPersisterer;
    private boolean enabled;


    public DokumentMottakerSøknadUng() {
    }

    @Inject
    public DokumentMottakerSøknadUng(SøknadParser søknadParser, MottatteDokumentRepository mottatteDokumentRepository, FagsakRepository fagsakRepository, UngdomsytelseSøknadPersisterer ungdomsytelseSøknadPersisterer,
                                     @KonfigVerdi(value = "UNGDOMSYTELSE_ENABLED", defaultVerdi = "false") boolean enabled) {
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.fagsakRepository = fagsakRepository;
        this.ungdomsytelseSøknadPersisterer = ungdomsytelseSøknadPersisterer;
        this.enabled = enabled;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        if (!enabled) {
            throw new IllegalStateException("Ytelsen er ikke skrudd på");
        }
        var behandlingId = behandling.getId();
        for (MottattDokument dokument : mottattDokument) {
            var søknad = søknadParser.parseSøknad(dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            if (søknad.getKildesystem().isPresent()) {
                dokument.setKildesystem(søknad.getKildesystem().get().getKode());
            }
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);

            var ytelse = søknad.getYtelse();
            var fom = ytelse.getSøknadsperiode().getFraOgMed();
            var tom = ytelse.getSøknadsperiode().getTilOgMed();
            if (tom == null) {
                throw new IllegalStateException("Søknad for ungdomsytelse må ha en sluttdato");
            }
            var faktiskSøknadsperiode = new Periode(fom, tom);
            ungdomsytelseSøknadPersisterer.lagreSøknadEntitet(søknad, dokument.getJournalpostId(), behandlingId, Optional.of(faktiskSøknadsperiode), dokument.getMottattDato());
            ungdomsytelseSøknadPersisterer.lagreSøknadsperioder(List.of(faktiskSøknadsperiode), dokument.getJournalpostId(), behandlingId);
            ungdomsytelseSøknadPersisterer.oppdaterFagsakperiode(Optional.of(faktiskSøknadsperiode), behandling.getFagsakId());
        }
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

}
