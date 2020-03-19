package no.nav.k9.sak.domene.medlem.impl;

import java.time.LocalDate;

import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

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
