package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingDtoUtil.get;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingDtoUtil.getFraMap;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        // for CDI proxy
    }

    @Inject
    public BehandlingDtoForBackendTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.vedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
    }

    public UtvidetBehandlingDto lagBehandlingDto(Behandling behandling, AsyncPollingStatus taskStatus) {
        var behandlingVedtak = vedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElse(null);

        return lagBehandlingDto(behandling, behandlingVedtak, taskStatus);
    }

    private UtvidetBehandlingDto lagBehandlingDto(Behandling behandling, BehandlingVedtak behandlingVedtak, AsyncPollingStatus asyncStatus) {
        UtvidetBehandlingDto dto = new UtvidetBehandlingDto();
        BehandlingDtoUtil.settStandardfelterUtvidet(behandling, dto, behandlingVedtak, erBehandlingGjeldendeVedtak(behandling));
        if (asyncStatus != null && !asyncStatus.isPending()) {
            dto.setAsyncStatus(asyncStatus);
        }

        UUID behandlingUuid = behandling.getUuid();
        Map<String, String> behandlinUuidQueryParams = Map.of(BehandlingUuidDto.NAME, behandlingUuid.toString());

        dto.leggTil(get(FagsakRestTjeneste.PATH, "fagsak", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));
        dto.leggTil(getFraMap(PersonRestTjeneste.PERSONOPPLYSNINGER_PATH, "soeker-personopplysninger", behandlinUuidQueryParams));
        dto.leggTil(getFraMap(PersonRestTjeneste.MEDLEMSKAP_V2_PATH, "medlemskap-v2", behandlinUuidQueryParams));
        dto.leggTil(getFraMap(SøknadRestTjeneste.SOKNAD_PATH, "soknad", behandlinUuidQueryParams));
        dto.leggTil(getFraMap(TilbakekrevingRestTjeneste.VARSELTEKST_PATH, "tilbakekrevingsvarsel-fritekst", behandlinUuidQueryParams));
        dto.leggTil(getFraMap(TilbakekrevingRestTjeneste.VALG_PATH, "tilbakekreving-valg", behandlinUuidQueryParams));

        behandling.getOriginalBehandling().ifPresent(originalBehandling -> {
            dto.leggTil(getFraMap(BehandlingBackendRestTjeneste.BEHANDLINGER_BACKEND_ROOT_PATH, "original-behandling",
                Map.of(BehandlingUuidDto.NAME, originalBehandling.getUuid().toString())));
        });

        setVedtakDato(dto, behandlingVedtak);
        if (behandlingVedtak != null) {
            setBehandlingsresultat(dto, behandling.getBehandlingsresultat());
        }
        dto.setSpråkkode(getSpråkkode(behandling));

        return dto;
    }

    private boolean erBehandlingGjeldendeVedtak(Behandling behandling) {
        Optional<BehandlingVedtak> gjeldendeVedtak = vedtakRepository.hentGjeldendeVedtak(behandling.getFagsak());
        return gjeldendeVedtak
            .filter(v -> v.getBehandlingId().equals(behandling.getId()))
            .isPresent();
    }

    private void setVedtakDato(UtvidetBehandlingDto dto, BehandlingVedtak behandlingsVedtak) {
        if(behandlingsVedtak!=null) {
            dto.setOriginalVedtaksDato(behandlingsVedtak.getVedtaksdato());
        }
    }

    private void setBehandlingsresultat(BehandlingDto dto, Behandlingsresultat behandlingsresultat) {
        BehandlingsresultatDto behandlingsresultatDto = new BehandlingsresultatDto();
        behandlingsresultatDto.setType(behandlingsresultat.getBehandlingResultatType());
        behandlingsresultatDto.setKonsekvenserForYtelsen(behandlingsresultat.getKonsekvenserForYtelsen());
        dto.setBehandlingsresultat(behandlingsresultatDto);
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
