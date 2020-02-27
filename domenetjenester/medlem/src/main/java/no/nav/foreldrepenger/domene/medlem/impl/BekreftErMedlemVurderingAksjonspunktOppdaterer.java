package no.nav.foreldrepenger.domene.medlem.impl;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public class BekreftErMedlemVurderingAksjonspunktOppdaterer {

    private MedlemskapRepository medlemskapRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public BekreftErMedlemVurderingAksjonspunktOppdaterer(BehandlingRepositoryProvider repositoryProvider, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void oppdater(Long behandlingId, BekreftErMedlemVurderingAksjonspunkt adapter) {

        var medlemskapManuellVurderingType = adapter.getManuellVurderingTypeKode();

        VurdertMedlemskapPeriodeEntitet.Builder vurdertMedlemskapPeriode = medlemskapRepository.hentBuilderFor(behandlingId);
        LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();

        var nytt = vurdertMedlemskapPeriode.getBuilderFor(skjæringstidspunkt)
            .medMedlemsperiodeManuellVurdering(medlemskapManuellVurderingType)
            .medBegrunnelse(adapter.getBegrunnelse());
        vurdertMedlemskapPeriode.leggTil(nytt);

        medlemskapRepository.lagreLøpendeMedlemskapVurdering(behandlingId, vurdertMedlemskapPeriode.build());
    }

}
