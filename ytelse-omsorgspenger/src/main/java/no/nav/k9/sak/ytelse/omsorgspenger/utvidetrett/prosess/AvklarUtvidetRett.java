package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.omsorgspenger.AvklarUtvidetRettDto;
import no.nav.k9.sak.typer.Periode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarUtvidetRettDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarUtvidetRett implements AksjonspunktOppdaterer<AvklarUtvidetRettDto> {

    private final VilkårType vilkårType = VilkårType.UTVIDETRETT;
    private final Avslagsårsak defaultAvslagsårsak = Avslagsårsak.IKKE_UTVIDETRETT;
    private final SkjermlenkeType skjermlenkeType = SkjermlenkeType.PUNKT_FOR_UTVIDETRETT;
    private final HistorikkEndretFeltType historikkEndretFeltType = HistorikkEndretFeltType.UTVIDETRETT;

    private HistorikkTjenesteAdapter historikkAdapter;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    AvklarUtvidetRett() {
        // for CDI proxy
    }

    @Inject
    AvklarUtvidetRett(HistorikkTjenesteAdapter historikkAdapter,
                      VilkårResultatRepository vilkårResultatRepository,
                      BehandlingRepository behandlingRepository) {
        this.historikkAdapter = historikkAdapter;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarUtvidetRettDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var fagsak = behandling.getFagsak();

        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var vilkårResultatBuilder = param.getVilkårResultatBuilder();

        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        var periode = dto.getPeriode();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var timeline = vilkårene.getVilkårTimeline(vilkårType);
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(vilkårType);

        boolean erAvslag = dto.getAvslagsårsak() != null;
        if (erAvslag || erÅpenPeriode(periode)) {
            // overskriver hele vilkårperioden
            // TODO: bør mulig avgrense fra søknad#mottattdato / søknadsperiode#fom i forbindelse med endringer?
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, timeline.getMinLocalDate(), timeline.getMaxLocalDate(), dto.getAvslagsårsak());
        } else {
            // overskriver bare avgrenset periode
            var tilbakestillPerioder = timeline.getLocalDateIntervals().stream().map(di -> DatoIntervallEntitet.fraOgMedTilOgMed(di.getFomDato(), di.getTomDato()))
                .collect(Collectors.toCollection(TreeSet::new));

            vilkårBuilder.tilbakestill(tilbakestillPerioder);
            var angittPeriode = validerAngittPeriode(fagsak, new LocalDateInterval(periode.getFom(), periode.getTom()));
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, angittPeriode.getFomDato(), angittPeriode.getTomDato(), dto.getAvslagsårsak());
        }

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        return OppdateringResultat.utenOveropp();
    }

    private boolean erÅpenPeriode(Periode periode) {
        return periode == null || !new LocalDateInterval(periode.getFom(), periode.getTom()).isClosedInterval();
    }

    private LocalDateInterval validerAngittPeriode(Fagsak fagsak, LocalDateInterval angittPeriode) {
        if (FagsakYtelseType.OMSORGSPENGER_KS == fagsak.getYtelseType()) {
            throw new UnsupportedOperationException("Kan ikke angi periode for ytelseType=" + fagsak.getYtelseType());
        }
        if (Objects.requireNonNull(angittPeriode).isOpenStart()) {
            throw new IllegalArgumentException("Angitt periode kan ikke ha åpen start. angitt=" + angittPeriode);
        }

        var fagsakPeriode = fagsak.getPeriode().toLocalDateInterval();
        if (!fagsakPeriode.contains(angittPeriode)) {
            throw new IllegalArgumentException("Angitt periode må være i det minste innenfor fagsakens periode. angitt=" + angittPeriode + ", fagsakPeriode=" + fagsakPeriode);
        }
        return angittPeriode;
    }

    private void oppdaterUtfallOgLagre(VilkårBuilder builder, Utfall utfallType, LocalDate fom, LocalDate tom, Avslagsårsak avslagsårsak) {
        Avslagsårsak settAvslagsårsak = !utfallType.equals(Utfall.OPPFYLT) ? (avslagsårsak == null ? defaultAvslagsårsak : avslagsårsak) : null;
        builder.leggTil(builder.hentBuilderFor(fom, tom)
            .medUtfallManuell(utfallType)
            .medAvslagsårsak(settAvslagsårsak));

    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Utfall nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(historikkEndretFeltType, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(skjermlenkeType);
    }
}
