package no.nav.foreldrepenger.web.app.tjenester.behandling;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.web.app.ApplicationConfig;
import no.nav.foreldrepenger.web.server.jetty.JettyWebKonfigurasjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.BehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingÅrsakDto;
import no.nav.k9.sak.kontrakt.behandling.UtvidetBehandlingDto;

public class BehandlingDtoUtil {

    static void settStandardfelterUtvidet(Behandling behandling, UtvidetBehandlingDto dto, boolean erBehandlingMedGjeldendeVedtak) {
        setStandardfelter(behandling, dto, erBehandlingMedGjeldendeVedtak);
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

    private static Optional<String> getVenteÅrsak(Behandling behandling) {
        Venteårsak venteårsak = behandling.getVenteårsak();
        if (venteårsak != null) {
            return Optional.of(venteårsak.getKode());
        }
        return Optional.empty();
    }

    private static List<BehandlingÅrsakDto> lagBehandlingÅrsakDto(Behandling behandling) {
        if (!behandling.getBehandlingÅrsaker().isEmpty()) {
            return behandling.getBehandlingÅrsaker().stream().map(BehandlingDtoUtil::map).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    static void setStandardfelter(Behandling behandling, BehandlingDto dto, boolean erBehandlingMedGjeldendeVedtak) {
        dto.setFagsakId(behandling.getFagsakId());
        dto.setId(behandling.getId());
        dto.setUuid(behandling.getUuid());
        dto.setVersjon(behandling.getVersjon());
        dto.setType(behandling.getType());
        dto.setOpprettet(behandling.getOpprettetDato());
        dto.setEndret(behandling.getEndretTidspunkt());
        dto.setEndretAvBrukernavn(behandling.getEndretAv());
        dto.setAvsluttet(behandling.getAvsluttetDato());
        dto.setStatus(behandling.getStatus());
        dto.setBehandlendeEnhetId(behandling.getBehandlendeOrganisasjonsEnhet().getEnhetId());
        dto.setBehandlendeEnhetNavn(behandling.getBehandlendeOrganisasjonsEnhet().getEnhetNavn());
        dto.setFørsteÅrsak(førsteÅrsak(behandling).orElse(null));
        dto.setGjeldendeVedtak(erBehandlingMedGjeldendeVedtak);
        dto.setBehandlingsfristTid(behandling.getBehandlingstidFrist());
        dto.setBehandlingPåVent(behandling.isBehandlingPåVent());
        getFristDatoBehandlingPåVent(behandling).ifPresent(dto::setFristBehandlingPåVent);
        getVenteÅrsak(behandling).ifPresent(dto::setVenteÅrsakKode);
        dto.setOriginalVedtaksDato(behandling.getOriginalVedtaksDato());
        dto.setAnsvarligSaksbehandler(behandling.getAnsvarligSaksbehandler());
        dto.setToTrinnsBehandling(behandling.isToTrinnsBehandling());
        dto.setBehandlingArsaker(lagBehandlingÅrsakDto(behandling));
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
    
    static ResourceLink post(String path, String rel, Object dto) {
        return ResourceLink.post(getApiPath(path), rel, dto);
    }
    
    private static String getApiPath() {
        String contextPath = JettyWebKonfigurasjon.CONTEXT_PATH;
        String apiUri = ApplicationConfig.API_URI;
        return contextPath + apiUri;
    }

    private static String getApiPath(String segment) {
        return getApiPath() + segment;
    }

}
