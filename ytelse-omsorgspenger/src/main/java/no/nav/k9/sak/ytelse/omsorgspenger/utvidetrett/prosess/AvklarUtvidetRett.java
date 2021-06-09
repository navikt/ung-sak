package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
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
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
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
    private SøknadRepository søknadRepository;

    AvklarUtvidetRett() {
        // for CDI proxy
    }

    @Inject
    AvklarUtvidetRett(HistorikkTjenesteAdapter historikkAdapter,
                      VilkårResultatRepository vilkårResultatRepository,
                      SøknadRepository søknadRepository,
                      BehandlingRepository behandlingRepository) {
        this.historikkAdapter = historikkAdapter;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadRepository = søknadRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarUtvidetRettDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var fagsak = behandling.getFagsak();

        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var vilkårResultatBuilder = param.getVilkårResultatBuilder();
        var periode = dto.getPeriode();

        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var originalVilkårTidslinje = vilkårene.getVilkårTimeline(vilkårType);

        // TODO: håndter delvis avslag fra en angitt dato ( eks. dersom tidligere innvilget, så innvilges på ny fra en nyere dato)?

        boolean erAvslag = dto.getAvslagsårsak() != null;
        var søknadsperiode = søknadRepository.hentSøknad(behandlingId).getSøknadsperiode();

        var perioder = new MinMaxPerioder(søknadsperiode, periode, originalVilkårTidslinje);
        if (periode != null) {
            vilkårResultatBuilder.slettVilkårPerioder(vilkårType, perioder.tilDatoIntervall());
        }

        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(vilkårType);

        LocalDate maksVilkårTom = originalVilkårTidslinje.getMaxLocalDate(); // siste dato vi trenger å overskrive til

        if (erAvslag) {
            // overskriver per nå hele vilkårperioden
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, perioder.minFom(), maksVilkårTom, dto.getAvslagsårsak());
        } else if (perioder.erÅpenPeriode(periode)) {
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, perioder.minFom(), maksVilkårTom, null /* avslagsårsak kan bare være null her */);
        } else {
            var angittPeriode = validerAngittPeriode(fagsak, new LocalDateInterval(periode.getFom(), periode.getTom()));
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, angittPeriode.getFomDato(), angittPeriode.getTomDato(), null /* avslagsårsak kan bare være null her */);
        }

        vilkårResultatBuilder.leggTil(vilkårBuilder);

        return OppdateringResultat.utenOveropp();
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

    record MinMaxPerioder(DatoIntervallEntitet søknadsperiode,
                          Periode angittPeriode,
                          LocalDateTimeline<VilkårPeriode> vilkårTimeline) {

        LocalDate minFom() {
            var fom = Stream.of(søknadsperiode.getFomDato(), angittPeriode.getFom(), vilkårTimeline.getMinLocalDate()).sorted(Comparator.nullsLast(Comparator.naturalOrder())).findFirst().get();
            return fom;
        }

        DatoIntervallEntitet tilDatoIntervall() {
            return DatoIntervallEntitet.fraOgMedTilOgMed(minFom(), maxTom());
        }

        LocalDate maxTom() {
            var tom = Stream.of(søknadsperiode.getTomDato(), angittPeriode.getTom(), vilkårTimeline.getMaxLocalDate()).sorted(Comparator.nullsLast(Comparator.reverseOrder())).findFirst().get();
            return tom;
        }

        boolean erÅpenPeriode(Periode periode) {
            return periode == null || !erLukketPeriode(periode);
        }

        boolean erLukketPeriode(Periode periode) {
            return periode != null && new LocalDateInterval(periode.getFom(), periode.getTom()).isClosedInterval();
        }
    }
}
