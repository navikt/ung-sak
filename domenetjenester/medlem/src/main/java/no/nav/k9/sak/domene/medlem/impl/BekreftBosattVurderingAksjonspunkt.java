package no.nav.k9.sak.domene.medlem.impl;

import java.time.LocalDate;

import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.kontrakt.medlem.BekreftBosattVurderingAksjonspunktDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public class BekreftBosattVurderingAksjonspunkt {

    private MedlemskapRepository medlemskapRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public BekreftBosattVurderingAksjonspunkt(BehandlingRepositoryProvider repositoryProvider, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void oppdater(Long behandlingId, BekreftBosattVurderingAksjonspunktDto adapter) {
        VurdertMedlemskapPeriodeEntitet.Builder vurdertMedlemskapPeriode = medlemskapRepository.hentBuilderFor(behandlingId);
        LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();

        vurdertMedlemskapPeriode.leggTil(vurdertMedlemskapPeriode.getBuilderFor(skjæringstidspunkt)
            .medBosattVurdering(adapter.getBosattVurdering())
            .medBegrunnelse(adapter.getBegrunnelse()));

        medlemskapRepository.lagreLøpendeMedlemskapVurdering(behandlingId, vurdertMedlemskapPeriode.build());
    }
}
