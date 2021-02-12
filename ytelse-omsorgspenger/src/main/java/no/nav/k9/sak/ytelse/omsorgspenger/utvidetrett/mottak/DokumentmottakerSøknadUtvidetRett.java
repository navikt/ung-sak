package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.mottak;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerUtvidetRett;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@DokumentGruppeRef(Brevkode.SØKNAD_OMS_UTVIDETRETT_KS_KODE)
@DokumentGruppeRef(Brevkode.SØKNAD_OMS_UTVIDETRETT_MA_KODE)
public class DokumentmottakerSøknadUtvidetRett implements Dokumentmottaker {

    private SøknadRepository søknadRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    private MottatteDokumentRepository mottatteDokumentRepository;

    DokumentmottakerSøknadUtvidetRett() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerSøknadUtvidetRett(BehandlingRepositoryProvider repositoryProvider,
                                      BehandlingRepository behandlingRepository,
                                      ProsessTaskRepository prosessTaskRepository,
                                      MottatteDokumentRepository mottatteDokumentRepository) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        Long behandlingId = behandling.getId();

        for (MottattDokument dokument : dokumenter) {
            Søknad søknad = JsonUtils.fromString(dokument.getPayload(), Søknad.class);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            // Søknadsinnhold som persisteres "lokalt" i k9-sak
            persister(søknad, behandling);
        }
    }

    void persister(Søknad søknad, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        var søknadInnhold = (OmsorgspengerUtvidetRett) søknad.getYtelse();

        lagreSøknad(behandlingId, søknad, søknadInnhold);
    }

    private void lagreSøknad(Long behandlingId, Søknad søknad, OmsorgspengerUtvidetRett søknadInnhold) {
        var søknadsperiode = søknadInnhold.getSøknadsperiode();
        boolean elektroniskSøknad = true;
        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFraOgMed(), søknadsperiode.getFraOgMed()))
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(søknad.getMottattDato().toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(søknad.getMottattDato().toLocalDate())
            .medSpråkkode(getSpråkValg(Språk.NORSK_BOKMÅL)) // TODO: hente riktig språk
        ;
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);
    }

    private Språkkode getSpråkValg(Språk språk) {
        if (språk != null) {
            return Språkkode.fraKode(språk.dto.toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

}
