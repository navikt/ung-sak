package no.nav.k9.sak.ytelse.frisinn.revurdering;

import java.time.LocalDate;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@Dependent
@FagsakYtelseTypeRef("FRISINN")
@BehandlingTypeRef("BT-004")
public class FrisinnRevurderingBehandlingsresultatutleder implements RevurderingBehandlingsresultatutleder {

    private BeregningTjeneste kalkulusTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttakRepository uttakRepository;

    @Inject
    public FrisinnRevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider, // NOSONAR
                                                        BeregningTjeneste beregningsgrunnlagTjeneste,
                                                        UttakRepository uttakRepository) {

        this.kalkulusTjeneste = beregningsgrunnlagTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.uttakRepository = uttakRepository;
    }


    @Override
    public VedtakVarsel bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef, VedtakVarsel vedtakVarsel, boolean erVarselOmRevurderingSendt) {
        Behandling revurdering = behandlingRepository.hentBehandling(revurderingRef.getBehandlingId());
        Behandling originalBehandling = revurdering.getOriginalBehandling().orElseThrow();

        return doBestemBehandlingsresultatForRevurdering(revurdering, originalBehandling, vedtakVarsel);
    }

    private VedtakVarsel doBestemBehandlingsresultatForRevurdering(Behandling revurdering, Behandling originalBehandling, VedtakVarsel vedtakVarsel) {
        var beregningsvilkår = vilkårResultatRepository.hent(revurdering.getId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var erNySøknadsperiode = erNySøknadperiode(revurdering, originalBehandling);
        var erEndringIBeregning = erEndringIBeregning(revurdering, originalBehandling, beregningsvilkår);
        var erHeltEllerDelvisInnvilget = erHeltEllerDelvisInnvilget(beregningsvilkår);

        // Oppdatering av Behandlingsresultat dersom betingelsene nedenfor inntreffer (ikke heldig å oppdatere verdi i ettertid)
        if (erHeltEllerDelvisInnvilget) {
            if (erNySøknadsperiode || erEndringIBeregning) {
                revurdering.setBehandlingResultatType(BehandlingResultatType.INNVILGET_ENDRING);
            } else {
                revurdering.setBehandlingResultatType(BehandlingResultatType.INGEN_ENDRING);
            }
        }
        
        // Oppdatering av vedtaksbrev
        if (!erNySøknadsperiode && revurdering.getBehandlingResultatType().equals(BehandlingResultatType.INGEN_ENDRING)) {
            vedtakVarsel.setVedtaksbrev(Vedtaksbrev.INGEN);
        } else {
            vedtakVarsel.setVedtaksbrev(Vedtaksbrev.AUTOMATISK);
        }
        return vedtakVarsel;
    }

    private boolean erHeltEllerDelvisInnvilget(Vilkår beregningsvilkår) {
        return beregningsvilkår.getPerioder().stream()
            .anyMatch(p -> p.getGjeldendeUtfall().equals(Utfall.OPPFYLT));
    }

    private boolean erEndringIBeregning(Behandling revurdering, Behandling originalBehandling, Vilkår beregningsvilkår) {
        boolean erEndringIBeregning = false;
        var skjæringstidspunkter = beregningsvilkår.getPerioder().stream().map(VilkårPeriode::getSkjæringstidspunkt).collect(Collectors.toList());
        for (LocalDate skjæringstidspunkt : skjæringstidspunkter) {
            var erEndring = kalkulusTjeneste.erEndringIBeregning(revurdering.getId(), originalBehandling.getId(), skjæringstidspunkt);
            if (erEndring) {
                erEndringIBeregning = erEndring;
                break;
            }
        }
        return erEndringIBeregning;
    }

    private boolean erNySøknadperiode(Behandling revurdering, Behandling origBehandling) {
        var nyttUttak = uttakRepository.hentFastsattUttak(revurdering.getId());
        var origUttak = uttakRepository.hentFastsattUttak(origBehandling.getId());

        return nyttUttak.getPerioder().size() > origUttak.getPerioder().size();
    }

}
