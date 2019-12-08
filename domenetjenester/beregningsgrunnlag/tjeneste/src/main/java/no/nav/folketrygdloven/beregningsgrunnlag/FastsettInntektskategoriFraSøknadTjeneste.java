package no.nav.folketrygdloven.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;

@ApplicationScoped
class FastsettInntektskategoriFraSøknadTjeneste {

    FastsettInntektskategoriFraSøknadTjeneste() {
        // for CDI proxy
    }

    void fastsettInntektskategori(BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag grunnlag) {
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .forEach(andel ->
                BeregningsgrunnlagPrStatusOgAndel.builder(andel).medInntektskategori(finnInntektskategoriForStatus(andel.getAktivitetStatus(), grunnlag)));
    }

    Optional<Inntektskategori> finnHøyestPrioriterteInntektskategoriForSN(List<Inntektskategori> inntektskategorier) {
        if (inntektskategorier.isEmpty()) { //NOSONAR Style - Method excessively uses methods of another class. Klassen fastsetter inntektskategori og prioritet av inntektskategori
            return Optional.empty();
        }
        if (inntektskategorier.size() == 1) {
            return Optional.of(inntektskategorier.get(0));
        }
        if (inntektskategorier.contains(Inntektskategori.FISKER)) {
            return Optional.of(Inntektskategori.FISKER);
        }
        if (inntektskategorier.contains(Inntektskategori.JORDBRUKER)) {
            return Optional.of(Inntektskategori.JORDBRUKER);
        }
        if (inntektskategorier.contains(Inntektskategori.DAGMAMMA)) {
            return Optional.of(Inntektskategori.DAGMAMMA);
        }
        return Optional.of(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    private Inntektskategori finnInntektskategoriForStatus(AktivitetStatus aktivitetStatus, InntektArbeidYtelseGrunnlag grunnlag) {
        if (SELVSTENDIG_NÆRINGSDRIVENDE.equals(aktivitetStatus)) {
            return finnInntektskategoriForSelvstendigNæringsdrivende(grunnlag);
        }
        return aktivitetStatus.getInntektskategori();
    }

    private Inntektskategori finnInntektskategoriForSelvstendigNæringsdrivende(InntektArbeidYtelseGrunnlag grunnlag) {
        Optional<OppgittOpptjening> oppgittOpptjening = grunnlag.getOppgittOpptjening();
        if (oppgittOpptjening.isPresent() && !oppgittOpptjening.get().getEgenNæring().isEmpty()) {
            Set<VirksomhetType> virksomhetTypeSet = oppgittOpptjening.get().getEgenNæring().stream()
                .map(OppgittEgenNæring::getVirksomhetType)
                .collect(Collectors.toSet());

            List<Inntektskategori> inntektskategorier = virksomhetTypeSet.stream()
                .map(v -> {
                    if (v.getInntektskategori() == Inntektskategori.UDEFINERT) {
                        return Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE;
                    }
                    return v.getInntektskategori();
                })
                .collect(Collectors.toList());

            return finnHøyestPrioriterteInntektskategoriForSN(inntektskategorier)
                .orElse(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        }
        return Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE;
    }
}
