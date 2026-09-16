package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bosted.VurderingAvBostedsvilkårEtterAvklaringDto;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt.BostedAvklaringTjeneste;

/**
 * Aksjonspunkt vurdering av bostedsvilkåret ved opphør eller avslått periode
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderingAvBostedsvilkårEtterAvklaringDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderingAvBostedsvilkårEtterAvklaringOppdaterer implements AksjonspunktOppdaterer<VurderingAvBostedsvilkårEtterAvklaringDto> {

    private VurderingAvVilkårEtterAvklaringTjeneste vurderingAvVilkårEtterAvklaringTjeneste;
    private BostedAvklaringTjeneste bostedAvklaringTjeneste;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    VurderingAvBostedsvilkårEtterAvklaringOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderingAvBostedsvilkårEtterAvklaringOppdaterer(VurderingAvVilkårEtterAvklaringTjeneste vurderingAvVilkårEtterAvklaringTjeneste,
                                                            @BehandlingÅrsakTypeRef(BehandlingÅrsakType.ENDRET_BOSTED) BostedAvklaringTjeneste bostedAvklaringTjeneste,
                                                            InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                                            InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                                            BehandlingRepository behandlingRepository,
                                                            HistorikkinnslagRepository historikkinnslagRepository) {
        this.vurderingAvVilkårEtterAvklaringTjeneste = vurderingAvVilkårEtterAvklaringTjeneste;
        this.bostedAvklaringTjeneste = bostedAvklaringTjeneste;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderingAvBostedsvilkårEtterAvklaringDto dto, AksjonspunktOppdaterParameter param) {
        long behandlingId = param.getBehandlingId();

        var resultatTidslinje = vurderingAvVilkårEtterAvklaringTjeneste.utled(
            behandlingId, VilkårType.BOSTEDSVILKÅR, dto.getVurdertePerioder(), hentÅrsakTidslinje(behandlingId));

        var periodeVurderinger = resultatTidslinje.segmenter().stream()
            .map(s -> new BostedsvilkårResultatPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue()))
            .toList();

        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandlingId, periodeVurderinger);
        inngangsvilkårVurderingTjeneste.settBostedsvilkårResultat(behandlingId, param.getVilkårResultatBuilder());

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        historikkinnslagRepository.lagre(new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Opphør av bostedsvilkåret vurdert")
            .build());

        return OppdateringResultat.nyttResultat();
    }

    private LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> hentÅrsakTidslinje(long behandlingId) {
        return new LocalDateTimeline<>(bostedAvklaringTjeneste.hentForeslåtteAvklaringer(behandlingId).stream()
            .map(a -> new LocalDateSegment<IkkeOppfyltDetaljertÅrsak>(
                a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a.getIkkeOppfyltÅrsak()))
            .toList());
    }
}
