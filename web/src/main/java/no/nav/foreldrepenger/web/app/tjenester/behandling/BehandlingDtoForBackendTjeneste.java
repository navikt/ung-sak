package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingDtoUtil.get;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.behandling.BehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingsresultatDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.behandling.UtvidetBehandlingDto;

/**
 * Returnerer behandlingsinformasjon og lenker for en behandling.
 * <p>
 * Tilsvarende tjeneste for front-end er @{@link BehandlingDtoTjeneste}
 * <p>
 * Det er valgt å skille de i to i håp om enklere vedlikehold av tjenesten for frontend.
 */

@ApplicationScoped
public class BehandlingDtoForBackendTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private SøknadRepository søknadRepository;

    public BehandlingDtoForBackendTjeneste() {
        //for CDI proxy
    }

    @Inject
    public BehandlingDtoForBackendTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.vedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
    }

    public UtvidetBehandlingDto lagBehandlingDto(Behandling behandling, AsyncPollingStatus taskStatus) {
        Optional<BehandlingVedtak> behandlingVedtak = vedtakRepository.hentBehandlingvedtakForBehandlingId(behandling.getId());

        return lagBehandlingDto(behandling, behandlingVedtak, taskStatus);
    }

    private UtvidetBehandlingDto lagBehandlingDto(Behandling behandling, Optional<BehandlingVedtak> behandlingVedtak, AsyncPollingStatus asyncStatus) {
        UtvidetBehandlingDto dto = new UtvidetBehandlingDto();
        BehandlingDtoUtil.settStandardfelterUtvidet(behandling, dto, erBehandlingGjeldendeVedtak(behandling));
        if (asyncStatus != null && !asyncStatus.isPending()) {
            dto.setAsyncStatus(asyncStatus);
        }

        var behandlingUuid = new BehandlingUuidDto(behandling.getUuid());

        dto.leggTil(get(FagsakRestTjeneste.PATH, "fagsak", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));
        dto.leggTil(get(PersonRestTjeneste.PERSONOPPLYSNINGER_PATH, "soeker-personopplysninger", behandlingUuid));
        dto.leggTil(get(PersonRestTjeneste.MEDLEMSKAP_V2_PATH, "medlemskap-v2", behandlingUuid));
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_PATH, "soknad", behandlingUuid));
        dto.leggTil(get(TilbakekrevingRestTjeneste.VARSELTEKST_PATH, "tilbakekrevingsvarsel-fritekst", behandlingUuid));
        dto.leggTil(get(TilbakekrevingRestTjeneste.VALG_PATH, "tilbakekreving-valg", behandlingUuid));

        behandling.getOriginalBehandling().ifPresent(originalBehandling -> {
            BehandlingUuidDto orginalBehandlingUuid = new BehandlingUuidDto(originalBehandling.getUuid());
            dto.leggTil(get(BehandlingBackendRestTjeneste.BEHANDLINGER_BACKEND_ROOT_PATH, "original-behandling", orginalBehandlingUuid));
        });

        setVedtakDato(dto, behandlingVedtak);
        setBehandlingsresultat(dto, behandlingVedtak);
        dto.setSpråkkode(getSpråkkode(behandling));

        return dto;
    }

    private boolean erBehandlingGjeldendeVedtak(Behandling behandling) {
        Optional<BehandlingVedtak> gjeldendeVedtak = vedtakRepository.hentGjeldendeVedtak(behandling.getFagsak());
        return gjeldendeVedtak
            .filter(v -> v.getBehandlingsresultat().getBehandling().getId().equals(behandling.getId()))
            .isPresent();
    }

    private void setVedtakDato(UtvidetBehandlingDto dto, Optional<BehandlingVedtak> behandlingsVedtak) {
        behandlingsVedtak.ifPresent(behandlingVedtak -> dto.setOriginalVedtaksDato(behandlingVedtak.getVedtaksdato()));
    }

    private void setBehandlingsresultat(BehandlingDto dto, Optional<BehandlingVedtak> behandlingsVedtak) {
        if (behandlingsVedtak.isPresent()) {
            Behandlingsresultat behandlingsresultat = behandlingsVedtak.get().getBehandlingsresultat();
            BehandlingsresultatDto behandlingsresultatDto = new BehandlingsresultatDto();
            behandlingsresultatDto.setType(behandlingsresultat.getBehandlingResultatType());
            behandlingsresultatDto.setKonsekvenserForYtelsen(behandlingsresultat.getKonsekvenserForYtelsen());
            dto.setBehandlingsresultat(behandlingsresultatDto);
        }
    }

    private Språkkode getSpråkkode(Behandling behandling) {
        Optional<SøknadEntitet> søknadOpt = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOpt.isPresent()) {
            return søknadOpt.get().getSpråkkode();
        } else {
            return behandling.getFagsak().getNavBruker().getSpråkkode();
        }
    }
}
