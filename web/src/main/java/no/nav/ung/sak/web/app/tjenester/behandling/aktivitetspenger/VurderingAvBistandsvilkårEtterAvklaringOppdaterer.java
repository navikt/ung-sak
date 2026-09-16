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
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VurderingAvBistandsvilkårEtterAvklaringDto;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;

import java.util.Set;

/**
 * Aksjonspunkt for vurdering av bistandsvilkåret ved opphør eller avslått periode.
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderingAvBistandsvilkårEtterAvklaringDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderingAvBistandsvilkårEtterAvklaringOppdaterer implements AksjonspunktOppdaterer<VurderingAvBistandsvilkårEtterAvklaringDto> {

    private VurderingAvVilkårEtterAvklaringTjeneste vurderingAvVilkårEtterAvklaringTjeneste;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    VurderingAvBistandsvilkårEtterAvklaringOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderingAvBistandsvilkårEtterAvklaringOppdaterer(VurderingAvVilkårEtterAvklaringTjeneste vurderingAvVilkårEtterAvklaringTjeneste,
                                                             VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                                             InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                                             InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                                             BehandlingRepository behandlingRepository,
                                                             HistorikkinnslagRepository historikkinnslagRepository) {
        this.vurderingAvVilkårEtterAvklaringTjeneste = vurderingAvVilkårEtterAvklaringTjeneste;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderingAvBistandsvilkårEtterAvklaringDto dto, AksjonspunktOppdaterParameter param) {
        long behandlingId = param.getBehandlingId();

        var resultatTidslinje = vurderingAvVilkårEtterAvklaringTjeneste.utled(
            behandlingId, VilkårType.BISTANDSVILKÅR, dto.getVurdertePerioder(), hentÅrsakTidslinje(behandlingId)
        );

        var periodeVurderinger = resultatTidslinje.segmenter().stream()
            .map(s -> new BistandsvilkårResultatPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue()))
            .toList();

        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(behandlingId, periodeVurderinger);
        inngangsvilkårVurderingTjeneste.settBistandsvilkårResultat(behandlingId, param.getVilkårResultatBuilder());

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        historikkinnslagRepository.lagre(new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.BISTANDSVILKÅR)
            .addLinje("Opphør av bistandsvilkåret vurdert")
            .build());

        return OppdateringResultat.nyttResultat();
    }

    private LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> hentÅrsakTidslinje(long behandlingId) {
        var foreslåtteAvklaringer = vilkårsavklaringGrunnlagRepository
            .hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of());

        return new LocalDateTimeline<>(foreslåtteAvklaringer.stream()
            .map(a -> new LocalDateSegment<>(
                a.getPeriode().getFomDato(),
                a.getPeriode().getTomDato(),
                IkkeOppfyltDetaljertÅrsak.fraKode(VilkårType.BISTANDSVILKÅR, a.getIkkeOppfyltÅrsakKode())))
            .toList());
    }
}
