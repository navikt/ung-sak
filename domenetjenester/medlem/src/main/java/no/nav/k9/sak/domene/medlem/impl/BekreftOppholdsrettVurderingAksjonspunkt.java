package no.nav.k9.sak.domene.medlem.impl;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.kontrakt.medlem.BekreftOppholdVurderingAksjonspunktDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public class BekreftOppholdsrettVurderingAksjonspunkt {

    private MedlemskapRepository medlemskapRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public BekreftOppholdsrettVurderingAksjonspunkt(BehandlingRepositoryProvider repositoryProvider, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void oppdater(Long behandlingId, BekreftOppholdVurderingAksjonspunktDto adapter) {
        VurdertMedlemskapPeriodeEntitet.Builder vurdertMedlemskapPeriode = medlemskapRepository.hentBuilderFor(behandlingId);
        LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();

        vurdertMedlemskapPeriode.leggTil(vurdertMedlemskapPeriode.getBuilderFor(skjæringstidspunkt)
            .medOppholdsrettVurdering(adapter.getOppholdsrettVurdering())
            .medLovligOppholdVurdering(adapter.getLovligOppholdVurdering())
            .medErEosBorger(adapter.getErEosBorger())
            .medBegrunnelse(adapter.getBegrunnelse()));

        medlemskapRepository.lagreLøpendeMedlemskapVurdering(behandlingId, vurdertMedlemskapPeriode.build());
    }
}
