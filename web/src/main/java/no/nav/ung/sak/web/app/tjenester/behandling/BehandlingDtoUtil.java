package no.nav.ung.sak.web.app.tjenester.behandling;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.kontrakt.ResourceLink;
import no.nav.ung.sak.kontrakt.ResourceLink.HttpMethod;
import no.nav.ung.sak.kontrakt.behandling.BehandlingDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingStegTilstandDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingÅrsakDto;
import no.nav.ung.sak.web.app.ApplicationConfig;
import no.nav.ung.sak.web.server.jetty.JettyWebKonfigurasjon;
import org.apache.hc.core5.net.URIBuilder;

public class BehandlingDtoUtil {

    static void settStandardfelterUtvidet(Behandling behandling, BehandlingDto dto, BehandlingVedtak behandlingVedtak,
                                          boolean erBehandlingMedGjeldendeVedtak) {
        setStandardfelter(behandling, dto, behandlingVedtak, erBehandlingMedGjeldendeVedtak);
        dto.setAnsvarligBeslutter(behandling.getAnsvarligBeslutter());
        dto.setBehandlingHenlagt(behandling.isBehandlingHenlagt());
    }

    private static Optional<String> getFristDatoBehandlingPåVent(Behandling behandling) {
        LocalDate frist = behandling.getFristDatoBehandlingPåVent();
        if (frist != null) {
            return Optional.of(frist.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); //$NON-NLS-1$
        }
        return Optional.empty();
    }

    private static Optional<Venteårsak> getVenteårsak(Behandling behandling) {
        Venteårsak venteårsak = behandling.getVenteårsak();
        return Optional.ofNullable(venteårsak);
    }

    private static List<BehandlingÅrsakDto> lagBehandlingÅrsakDto(Behandling behandling) {
        if (!behandling.getBehandlingÅrsaker().isEmpty()) {
            return behandling.getBehandlingÅrsaker().stream().map(BehandlingDtoUtil::map).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    static void setStandardfelter(Behandling behandling, BehandlingDto dto, BehandlingVedtak behandlingVedtak, boolean erBehandlingMedGjeldendeVedtak) {
        if (behandlingVedtak != null) {
            dto.setOriginalVedtaksDato(behandlingVedtak.getVedtaksdato());
        }

        dto.setFagsakId(behandling.getFagsakId());
        dto.setSakstype(behandling.getFagsakYtelseType());
        dto.setId(behandling.getId());
        dto.setUuid(behandling.getUuid());
        dto.setVersjon(behandling.getVersjon());
        dto.setType(behandling.getType());
        dto.setOpprettet(behandling.getOpprettetDato());
        dto.setEndret(behandling.getEndretTidspunkt());
        dto.setEndretAvBrukernavn(behandling.getEndretAv());
        dto.setAvsluttet(behandling.getAvsluttetDato());
        dto.setStatus(behandling.getStatus());
        dto.setGjeldendeVedtak(erBehandlingMedGjeldendeVedtak);
        dto.setBehandlingsfristTid(behandling.getBehandlingstidFrist());
        dto.setBehandlingPåVent(behandling.isBehandlingPåVent());
        dto.setAnsvarligSaksbehandler(behandling.getAnsvarligSaksbehandler());
        dto.setToTrinnsBehandling(behandling.isToTrinnsBehandling());
        dto.setBehandlingResultatType(behandling.getBehandlingResultatType());

        dto.setBehandlendeEnhetId(behandling.getBehandlendeOrganisasjonsEnhet().getEnhetId());
        dto.setBehandlendeEnhetNavn(behandling.getBehandlendeOrganisasjonsEnhet().getEnhetNavn());

        dto.setFørsteÅrsak(førsteÅrsak(behandling).orElse(null));
        dto.setBehandlingArsaker(lagBehandlingÅrsakDto(behandling));

        getFristDatoBehandlingPåVent(behandling).ifPresent(dto::setFristBehandlingPåVent);
        getVenteårsak(behandling).ifPresent(dto::setVenteårsak);
        behandling.getBehandlingStegTilstand().ifPresent(st -> {
            dto.setBehandlingStegTilstand(new BehandlingStegTilstandDto(st.getBehandlingSteg(), st.getBehandlingStegStatus(), st.getOpprettetTidspunkt()));
        });

    }

    static Optional<BehandlingÅrsakDto> førsteÅrsak(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .sorted(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt))
            .map(BehandlingDtoUtil::map)
            .findFirst();
    }

    private static BehandlingÅrsakDto map(BehandlingÅrsak årsak) {
        BehandlingÅrsakDto dto = new BehandlingÅrsakDto();
        dto.setBehandlingArsakType(årsak.getBehandlingÅrsakType());
        dto.setManueltOpprettet(årsak.erManueltOpprettet());
        return dto;
    }

    static ResourceLink get(String path, String rel, Object dto) {
        return ResourceLink.get(getApiPath(path), rel, dto);
    }

    static ResourceLink getFraMap(String path, String rel, Map<String, String> queryParams) {
        return ResourceLink.get(getApiPath(path), rel, queryParams);
    }

    static ResourceLink buildLink(String path, String rel, HttpMethod method, Consumer<URIBuilder> uriBuilder) {
        String apiPath = BehandlingDtoUtil.getApiPath(path);
        try {
            URIBuilder builder = new URIBuilder(apiPath);
            uriBuilder.accept(builder);
            return new ResourceLink(builder.build(), rel, method, false);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Kan ikke bygge uri for: " + path + ", rel=" + rel, e);
        }
    }

    static ResourceLink post(String path, String rel, Object dto) {
        return ResourceLink.post(getApiPath(path), rel, dto);
    }

    private static String getApiPath() {
        String contextPath = JettyWebKonfigurasjon.CONTEXT_PATH;
        String apiUri = ApplicationConfig.API_URI;
        return contextPath + apiUri;
    }

    public static String getApiPath(String segment) {
        return getApiPath() + segment;
    }

}
