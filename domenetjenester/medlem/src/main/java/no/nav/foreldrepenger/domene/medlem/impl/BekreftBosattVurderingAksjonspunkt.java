package no.nav.foreldrepenger.domene.medlem.impl;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.kontrakt.medlem.BekreftBosattVurderingAksjonspunktDto;

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
