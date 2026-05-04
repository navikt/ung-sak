package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.ManuellBostedPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.ManuellVurderingBostedsvilkårDto;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Oppdaterer for aksjonspunkt 5144 – manuell vurdering av bostedsvilkåret ved årsak ANNET.
 * Setter manuelt vurdert utfall (IKKE_OPPFYLT) med fritekstbegrunnelse direkte på vilkårsresultatet.
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = ManuellVurderingBostedsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellVurderingBostedsvilkårOppdaterer implements AksjonspunktOppdaterer<ManuellVurderingBostedsvilkårDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;

    ManuellVurderingBostedsvilkårOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ManuellVurderingBostedsvilkårOppdaterer(BehandlingRepository behandlingRepository,
                                                    HistorikkinnslagRepository historikkinnslagRepository,
                                                    VilkårResultatRepository vilkårResultatRepository,
                                                    BostedsGrunnlagRepository bostedsGrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
    }

    @Override
    public OppdateringResultat oppdater(ManuellVurderingBostedsvilkårDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        Map<LocalDate, String> begrunnelserPerSkjæringstidspunkt = dto.getPerioder().stream()
            .collect(Collectors.toMap(ManuellBostedPeriodeDto::getFom, ManuellBostedPeriodeDto::getBegrunnelse));

        var fastsattHolder = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag, behandlingId=" + behandlingId))
            .getFastsattHolder();
        if (fastsattHolder == null) {
            throw new IllegalStateException("Forventer fastsattHolder, behandlingId=" + behandlingId);
        }

        Map<LocalDate, BostedsPeriodeAvklaring> avklaringPerSkjæringstidspunkt = fastsattHolder.getPeriodeAvklaringer().stream()
            .filter(p -> FraflyttingsÅrsak.ANNET.equals(p.getFraflyttingsÅrsak()))
            .filter(p -> begrunnelserPerSkjæringstidspunkt.containsKey(p.getSkjæringstidspunkt()))
            .collect(Collectors.toMap(BostedsPeriodeAvklaring::getSkjæringstidspunkt, p -> p));

        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer vilkårresultat, behandlingId=" + behandlingId));

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);
        var vilkårTimeline = vilkårene.getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);

        for (var entry : avklaringPerSkjæringstidspunkt.entrySet()) {
            LocalDate skjæringstidspunkt = entry.getKey();
            BostedsPeriodeAvklaring avklaring = entry.getValue();
            String begrunnelse = begrunnelserPerSkjæringstidspunkt.get(skjæringstidspunkt);

            LocalDate ikkeOppfyltStart = (avklaring.isErBosattITrondheim() && avklaring.getFraflyttingsDato() != null)
                ? avklaring.getFraflyttingsDato()
                : skjæringstidspunkt;

            vilkårTimeline.stream()
                .filter(s -> s.getFom().equals(ikkeOppfyltStart))
                .findFirst()
                .ifPresent(s -> {
                    var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()));
                    periodeBuilder.medUtfallManuell(Utfall.IKKE_OPPFYLT)
                        .medAvslagsårsak(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED)
                        .medBegrunnelse(begrunnelse);
                    vilkårBuilder.leggTil(periodeBuilder);
                });
        }

        builder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Manuell vurdering av bostedsvilkåret lagret (årsak: Annet)")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }
}
