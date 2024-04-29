package no.nav.k9.sak.ytelse.pleiepengerbarn.medisinsk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleiebehovBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomAksjonspunkt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarMedisinskeOpplysningerDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarMedisinskeOpplysninger implements AksjonspunktOppdaterer<AvklarMedisinskeOpplysningerDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private SykdomVurderingTjeneste sykdomVurderingTjeneste;
    private MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private PleiebehovResultatRepository resultatRepository;
    private PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;

    AvklarMedisinskeOpplysninger() {
        // for CDI proxy
    }

    @Inject
    public AvklarMedisinskeOpplysninger(HistorikkTjenesteAdapter historikkTjenesteAdapter, SykdomVurderingTjeneste sykdomVurderingTjeneste,
                                        MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                        @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                        BehandlingRepository behandlingRepository, VilkårResultatRepository vilkårResultatRepository,
                                        PleiebehovResultatRepository resultatRepository, PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository,
                                        MedisinskGrunnlagRepository medisinskGrunnlagRepository) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.sykdomVurderingTjeneste = sykdomVurderingTjeneste;
        this.medisinskGrunnlagTjeneste = medisinskGrunnlagTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.resultatRepository = resultatRepository;
        this.pleietrengendeSykdomDokumentRepository = pleietrengendeSykdomDokumentRepository;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
    }


    @Override
    public OppdateringResultat oppdater(AvklarMedisinskeOpplysningerDto dto, AksjonspunktOppdaterParameter param) {
        final Behandling behandling = behandlingRepository.hentBehandling(param.getRef().getBehandlingId());
        VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = perioderTilVurderingTjeneste(behandling);

        boolean skalHaToTrinn;

        final var perioder = vilkårsPerioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        SykdomGrunnlagSammenlikningsresultat sammenlikningsresultat = medisinskGrunnlagTjeneste.utledRelevanteEndringerSidenForrigeBehandling(behandling, perioder);

        final boolean harTidligereHattRelevantGodkjentLegeerklæring = medisinskGrunnlagRepository.harHattGodkjentLegeerklæringMedUnntakAv(behandling.getFagsak().getPleietrengendeAktørId(), behandling.getUuid());
        final boolean harGodkjentLegeerklæring = !pleietrengendeSykdomDokumentRepository.hentGodkjenteLegeerklæringer(behandling.getFagsak().getPleietrengendeAktørId(), behandling.getFagsakYtelseType()).isEmpty();

        final var harFåttFørsteLegeerklæring = !harTidligereHattRelevantGodkjentLegeerklæring && harGodkjentLegeerklæring;
        final var harEndringer = !sammenlikningsresultat.getDiffPerioder().isEmpty();

        skalHaToTrinn = harEndringer || harFåttFørsteLegeerklæring;

        if (dto.isIkkeVentPåGodkjentLegeerklæring()) {
            final SykdomAksjonspunkt sykdomAksjonspunkt = sykdomVurderingTjeneste.vurderAksjonspunkt(behandling);
            if (!sykdomAksjonspunkt.isManglerGodkjentLegeerklæring()) {
                throw new IllegalStateException("Saksbehandler har bedt om å avslå sykdomsvilkåret grunnet manglende legeerklæring, selv om en godkjent legeerklæring allerede ligger inne.");
            }
            if (sykdomAksjonspunkt.isHarUklassifiserteDokumenter()) {
                throw new IllegalStateException("Det finnes uklassifiserte dokumenter på behandlingen. Disse må klassifiseres før man kan gi avslag grunnet manglende godkjent legeerklæring.");
            }

            /*
             * Vanligvis viser vi begrunnelsen fra saksbehandler som en del av historikkinnslaget, men dette
             * er ikke aktuelt for sykdom. Grunnen til dette er at vurderingene på sykdom gjøres på barnet.
             * 
             * Saksbehandler løser sykdomsaksjonspunktet i frontend ved å bare trykke "Fortsett", og det som
             * da skjer er at vi lager et grunnlag med opplysninger i saken. Dette er fint å dokumentere
             * gjennom et historikkinnslag, men det er da ingen begrunnelse for løsingen av aksjonspunktet.
             */
            lagHistorikkinnslag(param, "Sykdom manuelt behandlet: Mangler godkjent legeerklæring.");

            for (VilkårType vilkårType : vilkårsPerioderTilVurderingTjeneste.definerendeVilkår()) {
                Avslagsårsak avslagsårsak = utledAvslagsårsak(vilkårType);
                oppdaterMedIkkeOppfylt(vilkårType, avslagsårsak, param, behandling);
            }

            return OppdateringResultat.builder().medTotrinn().build();
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
        // Saksbehandler må igjen kvittere ut dokumenter manuelt for å sikre at de har blitt vurdert:
        //kvitterUtAlleDokumenterSomLiggerPåPleietrengende(behandling);

        lagHistorikkinnslag(param, "Sykdom manuelt behandlet.");

        final OppdateringResultat resultat = OppdateringResultat.builder().medTotrinnHvis(skalHaToTrinn).build();
        resultat.rekjørSteg();
        resultat.setSteg(BehandlingStegType.VURDER_MEDISINSKE_VILKÅR);

        return resultat;
    }

    private Avslagsårsak utledAvslagsårsak(VilkårType vilkårType) {
        return switch (vilkårType) {
            case MEDISINSKEVILKÅR_UNDER_18_ÅR,
                MEDISINSKEVILKÅR_18_ÅR -> Avslagsårsak.DOKUMENTASJON_IKKE_FRA_RETT_ORGAN;
            case I_LIVETS_SLUTTFASE -> Avslagsårsak.MANGLENDE_DOKUMENTASJON;
            default -> throw new IllegalArgumentException("Ikke-støttet VilkårType " + vilkårType);
        };
    }

    private void oppdaterMedIkkeOppfylt(VilkårType vilkårType, Avslagsårsak avslagsårsak, AksjonspunktOppdaterParameter param, final Behandling behandling) {
        var vilkårene = vilkårResultatRepository.hent(behandling.getId());
        var timeline = vilkårene.getVilkårTimeline(vilkårType);
        if (!timeline.isEmpty()) {
            var builder = param.getVilkårResultatBuilder();
            var vilkårBuilder = builder.hentBuilderFor(vilkårType);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(timeline.getMinLocalDate(), timeline.getMaxLocalDate())
                .medUtfallManuell(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(avslagsårsak));
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

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste(Behandling behandling) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
    }
}
