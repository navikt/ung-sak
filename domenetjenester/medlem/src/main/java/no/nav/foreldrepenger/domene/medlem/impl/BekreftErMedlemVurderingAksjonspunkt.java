package no.nav.foreldrepenger.domene.medlem.impl;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.kontrakt.medlem.BekreftErMedlemVurderingAksjonspunktDto;

public class BekreftErMedlemVurderingAksjonspunkt {

    private MedlemskapRepository medlemskapRepository;

    public BekreftErMedlemVurderingAksjonspunkt(BehandlingRepositoryProvider repositoryProvider) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    public void oppdater(Long behandlingId, BekreftErMedlemVurderingAksjonspunktDto adapter) {

        var medlemskapManuellVurderingType = adapter.getManuellVurderingTypeKode();

        Optional<VurdertMedlemskap> vurdertMedlemskap = medlemskapRepository.hentVurdertMedlemskap(behandlingId);

        var nytt = new VurdertMedlemskapBuilder(vurdertMedlemskap)
            .medMedlemsperiodeManuellVurdering(medlemskapManuellVurderingType)
            .medBegrunnelse(adapter.getBegrunnelse())
            .build();

        medlemskapRepository.lagreMedlemskapVurdering(behandlingId, nytt);
    }

}
