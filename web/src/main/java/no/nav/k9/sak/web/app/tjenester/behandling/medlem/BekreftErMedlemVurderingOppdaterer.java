package no.nav.k9.sak.web.app.tjenester.behandling.medlem;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.k9.sak.domene.medlem.impl.BekreftErMedlemVurderingAksjonspunkt;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medlem.BekreftErMedlemVurderingDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftedePerioderDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftErMedlemVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftErMedlemVurderingOppdaterer implements AksjonspunktOppdaterer<BekreftErMedlemVurderingDto> {
    private MedlemskapAksjonspunktTjeneste medlemTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private MedlemskapRepository medlemskapRepository;

    BekreftErMedlemVurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftErMedlemVurderingOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                              HistorikkTjenesteAdapter historikkAdapter,
                                              MedlemskapAksjonspunktTjeneste medlemTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.medlemTjeneste = medlemTjeneste;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    @Override
    public OppdateringResultat oppdater(BekreftErMedlemVurderingDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();

        Optional<BekreftedePerioderDto> bekreftedeDto = dto.getBekreftedePerioder().stream().findFirst();
        if (bekreftedeDto.isEmpty()) {
            return OppdateringResultat.nyttResultat();
        }
        BekreftedePerioderDto bekreftet = bekreftedeDto.get();
        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        Optional<VurdertMedlemskapPeriodeEntitet> løpendeVurderinger = medlemskap.flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap);
        final var vurdertMedlemskap = løpendeVurderinger.map(VurdertMedlemskapPeriodeEntitet::getPerioder)
            .orElse(Set.of())
            .stream()
            .filter(it -> it.getVurderingsdato().equals(param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt()))
            .findFirst();

        MedlemskapManuellVurderingType originalVurdering = vurdertMedlemskap.map(VurdertMedlemskap::getMedlemsperiodeManuellVurdering).orElse(null);
        MedlemskapManuellVurderingType bekreftetVurdering = bekreftet.getMedlemskapManuellVurderingType();
        String begrunnelseOrg = vurdertMedlemskap.map(VurdertMedlemskap::getBegrunnelse).orElse(null);

        String begrunnelse = bekreftet.getBegrunnelse();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, Objects.equals(begrunnelse, begrunnelseOrg))
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP);

        boolean erEndret = !Objects.equals(originalVurdering, bekreftetVurdering);
        if (erEndret) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.GYLDIG_MEDLEM_FOLKETRYGDEN, originalVurdering, bekreftetVurdering);
        }

        var adapter = new BekreftErMedlemVurderingAksjonspunkt(bekreftetVurdering, begrunnelse);
        medlemTjeneste.aksjonspunktBekreftMeldlemVurdering(behandlingId, adapter);

        return OppdateringResultat.builder().medTotrinnHvis(erEndret).build();
    }
}
