package no.nav.k9.sak.ytelse.pleiepengerbarn.medisinsk;

import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.AvklarMedisinskeOpplysningerDto;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleiebehovBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomAksjonspunkt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentHarOppdatertEksisterendeVurderinger;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PSBVilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarMedisinskeOpplysningerDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarMedisinskeOpplysninger implements AksjonspunktOppdaterer<AvklarMedisinskeOpplysningerDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private SykdomVurderingService sykdomVurderingService;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private PSBVilkårsPerioderTilVurderingTjeneste psbVilkårsPerioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private PleiebehovResultatRepository resultatRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;

    AvklarMedisinskeOpplysninger() {
        // for CDI proxy
    }

    @Inject
    public AvklarMedisinskeOpplysninger(HistorikkTjenesteAdapter historikkTjenesteAdapter, SykdomVurderingService sykdomVurderingService,
                                        SykdomGrunnlagService sykdomGrunnlagService, PSBVilkårsPerioderTilVurderingTjeneste psbVilkårsPerioderTilVurderingTjeneste,
                                        BehandlingRepository behandlingRepository, VilkårResultatRepository vilkårResultatRepository,
                                        PleiebehovResultatRepository resultatRepository, SykdomDokumentRepository sykdomDokumentRepository) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.sykdomVurderingService = sykdomVurderingService;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.psbVilkårsPerioderTilVurderingTjeneste = psbVilkårsPerioderTilVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.resultatRepository = resultatRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarMedisinskeOpplysningerDto dto, AksjonspunktOppdaterParameter param) {
        final Behandling behandling = behandlingRepository.hentBehandling(param.getRef().getBehandlingId());

        final var perioder = psbVilkårsPerioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        List<Periode> nyeVurderingsperioder = SykdomUtils.toPeriodeList(perioder);
        SykdomGrunnlagSammenlikningsresultat sammenlikningsresultat = sykdomGrunnlagService.utledRelevanteEndringerSidenForrigeBehandling(behandling, nyeVurderingsperioder);

        final var harEndringer = !sammenlikningsresultat.getDiffPerioder().isEmpty();
        final var skalHaToTrinn = harEndringer;


        if (dto.isIkkeVentPåGodkjentLegeerklæring()) {
            final SykdomAksjonspunkt sykdomAksjonspunkt = sykdomVurderingService.vurderAksjonspunkt(behandling);
            if (!sykdomAksjonspunkt.isManglerGodkjentLegeerklæring()) {
                throw new IllegalStateException("Saksbehandler har bedt om å avslå sykdomsvilkåret grunnet manglende legeerklæring, selv om en godkjent legeerklæring allerede ligger inne.");
            }
            if (sykdomAksjonspunkt.isHarUklassifiserteDokumenter()) {
                throw new IllegalStateException("Det finnes uklassifiserte dokumenter på behandlingen. Disse må klassifiseres før man kan gi avslag grunnet manglende godkjent legeerklæring.");
            }

            lagHistorikkinnslag(param, "Sykdom manuelt behandlet: Mangler godkjent legeerklæring.");

            oppdaterMedIkkeOppfylt(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, param, behandling);
            oppdaterMedIkkeOppfylt(VilkårType.MEDISINSKEVILKÅR_18_ÅR, param, behandling);
            return OppdateringResultat.utenOverhopp();
        }

        /*
         * Vi kvitterer her ut alle dokumenter som ligger på pleietrengende uavhengig av om
         * saksbehandler har sett dokumentet eller ikke. En bedre løsning er å enten:
         *
         * 1. Oppdatere behandlingsversjon når det kommer inn nye dokumenter.
         * 2. Sende med fra frontend hvilke dokumenter som skal kvitteres ut.
         *
         * ...men dette har ikke blitt prioritert.
         */
        kvitterUtAlleDokumenterSomLiggerPåPleietrengende(behandling);

        lagHistorikkinnslag(param, "Sykdom manuelt behandlet.");

        final OppdateringResultat resultat = OppdateringResultat.utenTransisjon().medTotrinnHvis(skalHaToTrinn).build();
        resultat.skalRekjøreSteg();
        resultat.setSteg(BehandlingStegType.VURDER_MEDISINSKVILKÅR);

        return resultat;
    }

    private void kvitterUtAlleDokumenterSomLiggerPåPleietrengende(final Behandling behandling) {
        final LocalDateTime nå = LocalDateTime.now();
        final List<SykdomDokument> dokumenter = sykdomDokumentRepository.hentDokumentSomIkkeHarOppdatertEksisterendeVurderinger(behandling.getFagsak().getPleietrengendeAktørId());
        for (var sykdomDokument : dokumenter) {
            sykdomDokumentRepository.kvitterDokumenterMedOppdatertEksisterendeVurderinger(new SykdomDokumentHarOppdatertEksisterendeVurderinger(sykdomDokument, SubjectHandler.getSubjectHandler().getUid(), nå));
        }
    }

    private void oppdaterMedIkkeOppfylt(VilkårType vilkårType, AksjonspunktOppdaterParameter param, final Behandling behandling) {
        var vilkårene = vilkårResultatRepository.hent(behandling.getId());
        var timeline = vilkårene.getVilkårTimeline(vilkårType);
        if (!timeline.isEmpty()) {
            var builder = param.getVilkårResultatBuilder();
            var vilkårBuilder = builder.hentBuilderFor(vilkårType);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(timeline.getMinLocalDate(), timeline.getMaxLocalDate())
                .medUtfallManuell(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(Avslagsårsak.DOKUMENTASJON_IKKE_FRA_RETT_ORGAN));
            builder.leggTil(vilkårBuilder);

            final var nåværendeResultat = resultatRepository.hentHvisEksisterer(behandling.getId());
            var resultatBuilder = nåværendeResultat.map(PleiebehovResultat::getPleieperioder).map(EtablertPleiebehovBuilder::builder).orElse(EtablertPleiebehovBuilder.builder());
            resultatBuilder.tilbakeStill(DatoIntervallEntitet.fraOgMedTilOgMed(timeline.getMinLocalDate(), timeline.getMaxLocalDate()));
            resultatRepository.lagreOgFlush(behandling.getId(), resultatBuilder);
        }
    }

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, String begrunnelse) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_MEDISINSK)
            .medBegrunnelse(begrunnelse);
        historikkTjenesteAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
    }
}
