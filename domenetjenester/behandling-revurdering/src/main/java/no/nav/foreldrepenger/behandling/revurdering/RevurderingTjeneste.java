package no.nav.foreldrepenger.behandling.revurdering;

import java.util.Collection;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;

public interface RevurderingTjeneste {

    Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, Optional<OrganisasjonsEnhet> enhet);

    Behandling opprettAutomatiskRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, Optional<OrganisasjonsEnhet> enhet);

    void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny);

    Boolean kanRevurderingOpprettes(Fagsak fagsak);

    boolean erRevurderingMedUendretUtfall(BehandlingReferanse ref, Collection<KonsekvensForYtelsen> konsekvenserForYtelsen);

}
