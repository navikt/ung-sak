package no.nav.k9.sak.ytelse.ung.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.ytelse.ung.periode.UngdomsprogramPeriode;
import no.nav.k9.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.olp.v1.Opplæringspenger;
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_SOKNAD_KODE)
public class DokumentMottakerSøknadUng implements Dokumentmottaker {

    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private boolean enabled;


    public DokumentMottakerSøknadUng() {
    }

    @Inject
    public DokumentMottakerSøknadUng(SøknadParser søknadParser, MottatteDokumentRepository mottatteDokumentRepository, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, @KonfigVerdi(value = "UNGDOMSYTELSE_ENABLED", defaultVerdi = "false") boolean enabled) {
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
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


            lagreSøknadsperioderSomUngdomsprogram(søknad, behandlingId);

        }
    }

    /** Lagrer søknadsperioder fra søknad som gyldige perioder med ungdomsprogram
     * @param søknad Søknad
     * @param behandlingId BehandlingId
     */
    // TODO: Ungdomsprogramperioder skal innhentes fra riktig kilde når dette er klart, men for testing lagrer vi ned perioder fra søknaden enn så lenge
    private void lagreSøknadsperioderSomUngdomsprogram(Søknad søknad, Long behandlingId) {
        Ungdomsytelse ytelse = søknad.getYtelse();
        var søknadsperiode = ytelse.getSøknadsperiode();
        ungdomsprogramPeriodeRepository.lagre(behandlingId, List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFraOgMed(), søknadsperiode.getTilOgMed()))));
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

}
