package no.nav.k9.sak.ytelse.ung.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.time.LocalDate;
import java.util.Collection;

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
import no.nav.k9.sak.ytelse.ung.periode.UtledSluttdato;
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_SOKNAD_KODE)
public class DokumentMottakerSøknadUng implements Dokumentmottaker {

    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private FagsakRepository fagsakRepository;
    private boolean enabled;


    public DokumentMottakerSøknadUng() {
    }

    @Inject
    public DokumentMottakerSøknadUng(SøknadParser søknadParser, MottatteDokumentRepository mottatteDokumentRepository, FagsakRepository fagsakRepository,
                                     @KonfigVerdi(value = "UNGDOMSYTELSE_ENABLED", defaultVerdi = "false") boolean enabled) {
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.fagsakRepository = fagsakRepository;
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
            var tom = UtledSluttdato.utledSluttdato(fom, ytelse.getSøknadsperiode().getTilOgMed());
            fagsakRepository.utvidPeriode(behandling.getFagsakId(), fom, tom);
        }
    }


    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

}
