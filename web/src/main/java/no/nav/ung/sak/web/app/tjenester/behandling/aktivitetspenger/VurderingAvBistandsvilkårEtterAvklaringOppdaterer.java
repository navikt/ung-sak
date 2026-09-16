package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VurderingAvBistandsvilkårEtterAvklaringDto;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.HistorikkinnslagInput;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;

import java.util.Set;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderingAvBistandsvilkårEtterAvklaringDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderingAvBistandsvilkårEtterAvklaringOppdaterer implements AksjonspunktOppdaterer<VurderingAvBistandsvilkårEtterAvklaringDto> {

    private static final VilkårType AKTUELT_VILKÅR = VilkårType.BISTANDSVILKÅR;

    private VurderingAvVilkårEtterAvklaringTjeneste vurderingAvVilkårEtterAvklaringTjeneste;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;

    VurderingAvBistandsvilkårEtterAvklaringOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderingAvBistandsvilkårEtterAvklaringOppdaterer(VurderingAvVilkårEtterAvklaringTjeneste vurderingAvVilkårEtterAvklaringTjeneste,
                                                             VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                                             InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                                             InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                                             VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste) {
        this.vurderingAvVilkårEtterAvklaringTjeneste = vurderingAvVilkårEtterAvklaringTjeneste;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.vilkårsvurderingHistorikkinnslagTjeneste = vilkårsvurderingHistorikkinnslagTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderingAvBistandsvilkårEtterAvklaringDto dto, AksjonspunktOppdaterParameter param) {
        long behandlingId = param.getBehandlingId();

        HistorikkinnslagInput historikkinnslagInput = vilkårsvurderingHistorikkinnslagTjeneste.hentInitielleVerdier(behandlingId, AKTUELT_VILKÅR);

        var resultatTidslinje = vurderingAvVilkårEtterAvklaringTjeneste.utled(
            behandlingId, AKTUELT_VILKÅR, dto.getVurdertePerioder(), hentÅrsakTidslinje(behandlingId)
        );

        var periodeVurderinger = resultatTidslinje.segmenter().stream()
            .map(s -> new BistandsvilkårResultatPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue()))
            .toList();

        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(behandlingId, periodeVurderinger);
        inngangsvilkårVurderingTjeneste.settBistandsvilkårResultat(behandlingId, param.getVilkårResultatBuilder());

        historikkinnslagInput.setSkjermlenkeType(SkjermlenkeType.BISTANDSVILKÅR)
            .setNyeVilkårVurderinger(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandlingId, AKTUELT_VILKÅR))
            .setHistorikkAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER);
        vilkårsvurderingHistorikkinnslagTjeneste.lagreHistorikkinnslag(historikkinnslagInput);

        return OppdateringResultat.nyttResultat();
    }

    private LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> hentÅrsakTidslinje(long behandlingId) {
        var foreslåtteAvklaringer = vilkårsavklaringGrunnlagRepository
            .hentGrunnlagHvisEksisterer(behandlingId, AKTUELT_VILKÅR)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of());

        return new LocalDateTimeline<>(foreslåtteAvklaringer.stream()
            .map(a -> new LocalDateSegment<>(
                a.getPeriode().getFomDato(),
                a.getPeriode().getTomDato(),
                IkkeOppfyltDetaljertÅrsak.fraKode(AKTUELT_VILKÅR, a.getIkkeOppfyltÅrsakKode())))
            .toList());
    }
}
