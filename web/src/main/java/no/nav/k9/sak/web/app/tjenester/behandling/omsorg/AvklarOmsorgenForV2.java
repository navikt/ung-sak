package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.omsorg.AvklarOmsorgenForDto;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForSaksbehandlervurdering;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOmsorgenForDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOmsorgenForV2 implements AksjonspunktOppdaterer<AvklarOmsorgenForDto> {

    private final SkjermlenkeType skjermlenkeType = SkjermlenkeType.FAKTA_OM_OMSORGENFOR;

    private HistorikkTjenesteAdapter historikkAdapter;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;

    
    AvklarOmsorgenForV2() {
        // for CDI proxy
    }

    @Inject
    AvklarOmsorgenForV2(HistorikkTjenesteAdapter historikkAdapter, OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository) {
        this.historikkAdapter = historikkAdapter;
        this.omsorgenForGrunnlagRepository = omsorgenForGrunnlagRepository;
    }
    

    @Override
    public OppdateringResultat oppdater(AvklarOmsorgenForDto dto, AksjonspunktOppdaterParameter param) {       
        // TODO Omsorg: Hvordan håndtere nye vurderinger på utsiden av perioderTilVurdering? Stoppe vurderinger på utsiden av søknadsperiode?
        Long behandlingId = param.getBehandlingId();
        
        // TODO Omsorg: Løkke over endringene slik at vi får bedre historikk=
        lagHistorikkInnslag(param, "Omsorg manuelt behandlet.");
        
        final List<OmsorgenForSaksbehandlervurdering> nyeVurderinger = toOmsorgenForSaksbehandlervurderinger(dto);
        omsorgenForGrunnlagRepository.lagreNyeVurderinger(behandlingId, nyeVurderinger);

        return OppdateringResultat.utenOveropp();
    }

    private List<OmsorgenForSaksbehandlervurdering> toOmsorgenForSaksbehandlervurderinger(AvklarOmsorgenForDto dto) {
        return dto.getOmsorgsperioder()
                .stream()
                .map(op -> new OmsorgenForSaksbehandlervurdering(
                    DatoIntervallEntitet.fraOgMedTilOgMed(op.getPeriode().getFom(), op.getPeriode().getTom()),
                    op.getBegrunnelse(),
                    op.getResultat()
                ))
                .collect(Collectors.toList());
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, String begrunnelse) {
        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(skjermlenkeType);
    }
}
