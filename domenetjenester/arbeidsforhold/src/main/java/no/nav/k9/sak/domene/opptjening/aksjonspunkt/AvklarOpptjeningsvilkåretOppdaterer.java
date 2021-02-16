package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.opptjening.Opptjeningsfeil;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.opptjening.AvklarOpptjeningsvilkårDto;
import no.nav.k9.sak.kontrakt.opptjening.AvklarOpptjeningsvilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOpptjeningsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOpptjeningsvilkåretOppdaterer implements AksjonspunktOppdaterer<AvklarOpptjeningsvilkårDto> {

    private OpptjeningRepository opptjeningRepository;
    private HistorikkTjenesteAdapter historikkAdapter;

    AvklarOpptjeningsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOpptjeningsvilkåretOppdaterer(OpptjeningRepository opptjeningRepository,
                                               HistorikkTjenesteAdapter historikkAdapter) {

        this.opptjeningRepository = opptjeningRepository;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOpptjeningsvilkårDto dto, AksjonspunktOppdaterParameter param) {
        var builder = param.getVilkårResultatBuilder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);
        for (AvklarOpptjeningsvilkåretDto avklarOpptjeningsvilkåretDto : dto.getPerioder()) {
            Utfall nyttUtfall = avklarOpptjeningsvilkåretDto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;

            lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

            if (nyttUtfall.equals(Utfall.OPPFYLT)) {
                var periode = DatoIntervallEntitet.fraOgMedTilOgMed(avklarOpptjeningsvilkåretDto.getOpptjeningFom(), avklarOpptjeningsvilkåretDto.getOpptjeningTom());
                sjekkOmVilkåretKanSettesTilOppfylt(param.getBehandlingId(), periode);
            }
            oppdaterUtfallOgLagre(nyttUtfall, avklarOpptjeningsvilkåretDto.getOpptjeningFom(), avklarOpptjeningsvilkåretDto.getOpptjeningTom(), vilkårBuilder);
        }
        builder.leggTil(vilkårBuilder);
        return OppdateringResultat.utenOveropp();
    }

    private void oppdaterUtfallOgLagre(Utfall utfallType, LocalDate fom, LocalDate tom, VilkårBuilder vilkårBuilder) {
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
            .medUtfall(utfallType)
            .medAvslagsårsak(!utfallType.equals(Utfall.OPPFYLT) ? Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING : null));
    }

    private void sjekkOmVilkåretKanSettesTilOppfylt(Long behandlingId, DatoIntervallEntitet periode) {
        var opptjening = opptjeningRepository.finnOpptjening(behandlingId).flatMap(it -> it.finnOpptjening(periode));
        if (opptjening.isPresent()) {
            long antall = opptjening.get().getOpptjeningAktivitet().stream()
                .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD)).count();
            if (antall > 0) {
                return;
            }
        }
        throw Opptjeningsfeil.FACTORY.opptjeningPreconditionFailed().toException();
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Utfall nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.OPPTJENINGSVILKARET, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }
}
