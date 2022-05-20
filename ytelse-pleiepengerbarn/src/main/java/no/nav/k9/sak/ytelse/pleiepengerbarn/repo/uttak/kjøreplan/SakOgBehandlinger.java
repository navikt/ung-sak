package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

class SakOgBehandlinger {
    private final Long fagsak;
    private final Long behandling;
    private final Map<Long, BehandlingMedMetadata> behandlingerMedMetadata;

    private final List<MottattKrav> mottattDokumenter;
    private final Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter;
    private Saksnummer saksnummer;

    public SakOgBehandlinger(Long fagsak, Saksnummer saksnummer, Long behandling, Map<Long, BehandlingMedMetadata> behandlingerMedMetadata, List<MottattKrav> mottattDokumenter, Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter) {
        this.fagsak = fagsak;
        this.saksnummer = saksnummer;
        this.behandling = behandling;
        this.behandlingerMedMetadata = behandlingerMedMetadata;
        this.mottattDokumenter = mottattDokumenter;
        this.kravDokumenter = kravDokumenter;
    }

    public Long getFagsak() {
        return fagsak;
    }

    public Optional<Long> getBehandling() {
        return Optional.ofNullable(behandling);
    }

    public List<MottattKrav> getMottattDokumenter() {
        return mottattDokumenter;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> getKravDokumenter() {
        return kravDokumenter;
    }

    public Set<Long> getBehandlinger() {
        return behandlingerMedMetadata.keySet();
    }

    public BehandlingStatus getBehandlingStatus(Long behandlingId) {
        var behandlingMedMetadata = behandlingerMedMetadata.getOrDefault(behandlingId, null);
        if (behandlingMedMetadata == null) {
            return null;
        }
        return behandlingMedMetadata.getBehandlingStatus();
    }

    public Optional<Long> getOriginalBehandling(Long behandlingId) {
        var behandlingMedMetadata = behandlingerMedMetadata.getOrDefault(behandlingId, null);
        if (behandlingMedMetadata == null) {
            return Optional.empty();
        }
        return behandlingMedMetadata.getOrginalBehandling();
    }

    public Optional<Long> getEtterfølgendeBehandling(Long behandlingId) {
        var etterfølgendeBehandlinger = behandlingerMedMetadata.entrySet().stream().filter(it -> it.getValue().getOrginalBehandling().map(ob -> Objects.equals(ob, behandlingId)).orElse(false))
            .findFirst();
        if (etterfølgendeBehandlinger.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(etterfølgendeBehandlinger.get().getKey());
    }
}
