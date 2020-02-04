package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.k9.kodeverk.iay.AktivitetStatus;

@ApplicationScoped
public class AndelGraderingTjeneste {

    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    AndelGraderingTjeneste() {
        //CDI
    }

    @Inject
    public AndelGraderingTjeneste(HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }
    
    public AktivitetGradering utled(BehandlingReferanse ref) {
        return new AktivitetGradering(utledAndeler(ref));
    }

    private List<AndelGradering> utledAndeler(BehandlingReferanse ref) {

        // FIXME K9 trengs gradering perioder her?  Fra avkorting mot arbeidsforhold?
        List<PeriodeMedGradering> perioderMedGradering = List.of();

        return map(perioderMedGradering, ref);
    }

    private List<AndelGradering> map(List<PeriodeMedGradering> perioderMedGradering, BehandlingReferanse ref) {
        Map<AndelGradering, AndelGradering.Builder> map = new HashMap<>();
        perioderMedGradering.forEach(periodeMedGradering -> {
            AktivitetStatus aktivitetStatus = periodeMedGradering.aktivitetStatus;
            AndelGradering.Builder nyBuilder = AndelGradering.builder()
                .medStatus(aktivitetStatus);
            if (AktivitetStatus.ARBEIDSTAKER.equals(aktivitetStatus)) {
                Arbeidsgiver arbeidsgiver = periodeMedGradering.arbeidsgiver;
                Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
                nyBuilder.medArbeidsgiver(arbeidsgiver);
            }
            AndelGradering andelGradering = nyBuilder.build();

            AndelGradering.Builder builder = map.get(andelGradering);
            if (builder == null) {
                builder = nyBuilder;
                map.put(andelGradering, nyBuilder);
            }

            if (aktivitetStatus.erSelvstendigNæringsdrivende()) {
                settAndelsnrForStatus(ref, builder, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
            } else if (aktivitetStatus.erFrilanser()) {
                settAndelsnrForStatus(ref, builder, AktivitetStatus.FRILANSER);
            }

            builder.leggTilGradering(mapGradering(periodeMedGradering));
        });
        return new ArrayList<>(map.keySet());
    }

    private void settAndelsnrForStatus(BehandlingReferanse ref, AndelGradering.Builder builder, AktivitetStatus status) {
        Optional<BeregningsgrunnlagPrStatusOgAndel> matchetAndel = finnAndelIGrunnlag(status, ref);
        matchetAndel.ifPresent(beregningsgrunnlagPrStatusOgAndel -> builder.medAndelsnr(beregningsgrunnlagPrStatusOgAndel.getAndelsnr()));
    }

    private Optional<BeregningsgrunnlagPrStatusOgAndel> finnAndelIGrunnlag(AktivitetStatus aktivitetstatus, BehandlingReferanse ref) {
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagForBehandling(ref.getBehandlingId());
        if (!beregningsgrunnlag.isPresent()) {
            return Optional.empty();
        }
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.get().getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        return andeler.stream().filter(andel -> andel.getAktivitetStatus().equals(aktivitetstatus)).findFirst();
    }

    private Gradering mapGradering(PeriodeMedGradering periodeMedGradering) {
        return new AndelGradering.Gradering(periodeMedGradering.fom, periodeMedGradering.tom, periodeMedGradering.arbeidsprosent);
    }

    public static class PeriodeMedGradering {

        private final LocalDate fom;
        private final LocalDate tom;
        private final BigDecimal arbeidsprosent;
        private final AktivitetStatus aktivitetStatus;
        private final Arbeidsgiver arbeidsgiver;

        PeriodeMedGradering(LocalDate fom,
                            LocalDate tom,
                            BigDecimal arbeidsprosent,
                            AktivitetStatus aktivitetStatus,
                            Arbeidsgiver arbeidsgiver) {

            this.fom = fom;
            this.tom = tom;
            this.arbeidsprosent = arbeidsprosent;
            this.aktivitetStatus = aktivitetStatus;
            this.arbeidsgiver = arbeidsgiver;
        }
    }
}
