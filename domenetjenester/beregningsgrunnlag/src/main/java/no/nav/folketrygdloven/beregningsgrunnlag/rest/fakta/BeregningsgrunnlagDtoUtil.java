package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelBeregningsgrunnlagArbeidsforholdDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.FinnNavnForManueltLagtTilArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;

@ApplicationScoped
public class BeregningsgrunnlagDtoUtil {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    public BeregningsgrunnlagDtoUtil() {
        // CDI
    }

    @Inject
    public BeregningsgrunnlagDtoUtil(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                     ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    FaktaOmBeregningAndelDto lagFaktaOmBeregningAndel(BeregningsgrunnlagPrStatusOgAndel andel,
                                                      AktivitetGradering aktivitetGradering,
                                                      InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        FaktaOmBeregningAndelDto andelDto = settVerdierForAndel(andel, aktivitetGradering, inntektArbeidYtelseGrunnlag);
        andelDto.setAndelsnr(andel.getAndelsnr());
        return andelDto;
    }

    FaktaOmBeregningAndelDto lagFaktaOmBeregningAndel(BeregningsgrunnlagPrStatusOgAndel andel,
                                                      Optional<BeregningsgrunnlagPrStatusOgAndel> andelFraForrigeKofakBerOpt,
                                                      AktivitetGradering aktivitetGradering,
                                                      InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        FaktaOmBeregningAndelDto andelDto;
        if (andelFraForrigeKofakBerOpt.isPresent()) {
            andelDto = settVerdierForAndel(andelFraForrigeKofakBerOpt.get(), aktivitetGradering, inntektArbeidYtelseGrunnlag);
            andelDto.setAndelsnr(andel.getAndelsnr());
            return andelDto;
        }
        andelDto = settVerdierForAndel(andel, aktivitetGradering, inntektArbeidYtelseGrunnlag);
        andelDto.setAndelsnr(andel.getAndelsnr());
        return andelDto;
    }

    private FaktaOmBeregningAndelDto settVerdierForAndel(BeregningsgrunnlagPrStatusOgAndel andel, AktivitetGradering aktivitetGradering, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        FaktaOmBeregningAndelDto andelDto = new FaktaOmBeregningAndelDto();
        andelDto.setAktivitetStatus(andel.getAktivitetStatus());
        andelDto.setInntektskategori(andel.getInntektskategori());
        andelDto.setFastsattAvSaksbehandler(andel.getFastsattAvSaksbehandler());
        andelDto.setLagtTilAvSaksbehandler(andel.getLagtTilAvSaksbehandler());
        List<Gradering> graderingForAndelIPeriode = FordelBeregningsgrunnlagTjeneste.hentGraderingerForAndelIPeriode(andel, aktivitetGradering).stream()
            .sorted().collect(Collectors.toList());
        finnArbeidsprosenterIPeriode(graderingForAndelIPeriode, andel.getBeregningsgrunnlagPeriode().getPeriode()).forEach(andelDto::leggTilAndelIArbeid);
        lagArbeidsforholdDto(andel, Optional.empty(), inntektArbeidYtelseGrunnlag)
            .ifPresent(andelDto::setArbeidsforhold);
        return andelDto;
    }


    public Optional<BeregningsgrunnlagArbeidsforholdDto> lagArbeidsforholdDto(BeregningsgrunnlagPrStatusOgAndel andel, Optional<Inntektsmelding> inntektsmeldingOptional, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        BeregningsgrunnlagArbeidsforholdDto dto = new BeregningsgrunnlagArbeidsforholdDto();
        return lagBeregningsgrunnlagArbeidsforholdDto(andel, dto, inntektsmeldingOptional, inntektArbeidYtelseGrunnlag);
    }

    Optional<BeregningsgrunnlagArbeidsforholdDto> lagArbeidsforholdEndringDto(BeregningsgrunnlagPrStatusOgAndel andel, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        BeregningsgrunnlagArbeidsforholdDto dto = new FordelBeregningsgrunnlagArbeidsforholdDto();
        return lagBeregningsgrunnlagArbeidsforholdDto(andel, dto, Optional.empty(), inntektArbeidYtelseGrunnlag);
    }

    private Optional<BeregningsgrunnlagArbeidsforholdDto> lagBeregningsgrunnlagArbeidsforholdDto(BeregningsgrunnlagPrStatusOgAndel andel,
                                                                                                 BeregningsgrunnlagArbeidsforholdDto arbeidsforhold,
                                                                                                 Optional<Inntektsmelding> inntektsmeldingOptional, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        if (skalIkkeOppretteArbeidsforhold(andel)) {
            return Optional.empty();
        }
        mapBgAndelArbeidsforhold(andel, arbeidsforhold, inntektsmeldingOptional, inntektArbeidYtelseGrunnlag);
        arbeidsforhold.setArbeidsforholdType(andel.getArbeidsforholdType());
        return Optional.of(arbeidsforhold);
    }

    private boolean skalIkkeOppretteArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel) {
        boolean arbeidsforholdTypeErIkkeSatt = andel.getArbeidsforholdType() == null
            || OpptjeningAktivitetType.UDEFINERT.equals(andel.getArbeidsforholdType());
        return arbeidsforholdTypeErIkkeSatt && !andel.getBgAndelArbeidsforhold().isPresent();

    }

    private void mapBgAndelArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel,
                                          BeregningsgrunnlagArbeidsforholdDto arbeidsforhold,
                                          Optional<Inntektsmelding> inntektsmelding,
                                          InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        andel.getBgAndelArbeidsforhold().ifPresent(bga -> {
            Arbeidsgiver arbeidsgiver = bga.getArbeidsgiver();
            arbeidsforhold.setStartdato(bga.getArbeidsperiodeFom());
            arbeidsforhold.setOpphoersdato(finnKorrektOpphørsdato(andel));
            arbeidsforhold.setArbeidsforholdId(bga.getArbeidsforholdRef().getReferanse());
            inntektArbeidYtelseGrunnlag.getArbeidsforholdInformasjon().ifPresent(arbeidsforholdInformasjon -> {
                var eksternArbeidsforholdRef = arbeidsforholdInformasjon.finnEkstern(arbeidsgiver, bga.getArbeidsforholdRef());
                arbeidsforhold.setEksternArbeidsforholdId(eksternArbeidsforholdRef.getReferanse());
            });
            arbeidsforhold.setRefusjonPrAar(bga.getRefusjonskravPrÅr());
            arbeidsforhold.setNaturalytelseBortfaltPrÅr(bga.getNaturalytelseBortfaltPrÅr().orElse(null));
            arbeidsforhold.setNaturalytelseTilkommetPrÅr(bga.getNaturalytelseTilkommetPrÅr().orElse(null));
            inntektsmelding.ifPresent(im -> arbeidsforhold.setBelopFraInntektsmeldingPrMnd(im.getInntektBeløp().getVerdi()));
            mapArbeidsgiver(arbeidsforhold, arbeidsgiver, inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer());
        });
    }

    private String hentNavnTilManueltArbeidsforhold(List<ArbeidsforholdOverstyring> overstyringer) {
        return FinnNavnForManueltLagtTilArbeidsforholdTjeneste.finnNavnTilManueltLagtTilArbeidsforhold(overstyringer)
            .map(ArbeidsgiverOpplysninger::getNavn)
            .orElseThrow(() -> new IllegalStateException("Fant ikke forventet informasjon om manuelt arbeidsforhold"));
    }

    private void mapArbeidsgiver(BeregningsgrunnlagArbeidsforholdDto arbeidsforhold, Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        if (opplysninger != null) {
            if (arbeidsgiver.getErVirksomhet()) {
                if (Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
                    arbeidsforhold.setArbeidsgiverNavn(hentNavnTilManueltArbeidsforhold(overstyringer));
                    arbeidsforhold.setArbeidsgiverId(arbeidsgiver.getOrgnr());
                    arbeidsforhold.setOrganisasjonstype(Organisasjonstype.KUNSTIG);
                } else {
                    Virksomhet virksomhet = arbeidsgiverTjeneste.hentVirksomhet(arbeidsgiver.getOrgnr());
                    arbeidsforhold.setArbeidsgiverNavn(virksomhet.getNavn());
                    arbeidsforhold.setArbeidsgiverId(virksomhet.getOrgnr());
                    arbeidsforhold.setOrganisasjonstype(virksomhet.getOrganisasjonstype());
                }
            } else if (arbeidsgiver.erAktørId()) {
                arbeidsforhold.setArbeidsgiverNavn(opplysninger.getNavn());
                arbeidsforhold.setArbeidsgiverId(opplysninger.getIdentifikator());
                arbeidsforhold.setAktørId(arbeidsgiver.getAktørId());
            }
        }
    }

    private LocalDate finnKorrektOpphørsdato(BeregningsgrunnlagPrStatusOgAndel andel) {
        return andel.getBgAndelArbeidsforhold()
            .flatMap(BGAndelArbeidsforhold::getArbeidsperiodeTom)
            .filter(tom -> !TIDENES_ENDE.equals(tom))
            .orElse(null);
    }

    Optional<BeregningsgrunnlagPeriode> finnMatchendePeriodeIForrigeBeregningsgrunnlag(BehandlingReferanse ref, BeregningsgrunnlagPeriode periode,
                                                                                       BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        Optional<BeregningsgrunnlagEntitet> forrigeGrunnlag = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(ref.getBehandlingId(), ref.getOriginalBehandlingId(),
            beregningsgrunnlagTilstand).flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        if (!forrigeGrunnlag.isPresent()) {
            return Optional.empty();
        }
        List<BeregningsgrunnlagPeriode> matchedePerioder = forrigeGrunnlag.get()
            .getBeregningsgrunnlagPerioder().stream()
            .filter(periodeIGjeldendeGrunnlag -> periode.getPeriode().overlapper(periodeIGjeldendeGrunnlag.getPeriode())).collect(Collectors.toList());
        if (matchedePerioder.size() == 1) {
            return Optional.of(matchedePerioder.get(0));
        }
        return Optional.empty();
    }

    List<BigDecimal> finnArbeidsprosenterIPeriode(List<Gradering> graderingForAndelIPeriode, ÅpenDatoIntervallEntitet periode) {
        List<BigDecimal> prosentAndelerIPeriode = new ArrayList<>();
        if (graderingForAndelIPeriode.isEmpty()) {
            leggTilNullProsent(prosentAndelerIPeriode);
            return prosentAndelerIPeriode;
        }
        Gradering gradering = graderingForAndelIPeriode.get(0);
        prosentAndelerIPeriode.add(gradering.getArbeidstidProsent());
        if (graderingForAndelIPeriode.size() == 1) {
            if (graderingDekkerHeilePerioden(gradering, periode)) {
                return prosentAndelerIPeriode;
            }
            prosentAndelerIPeriode.add(BigDecimal.ZERO);
            return prosentAndelerIPeriode;
        }

        for (int i = 1; i < graderingForAndelIPeriode.size(); i++) {
            Gradering nesteGradering = graderingForAndelIPeriode.get(i);
            prosentAndelerIPeriode.add(nesteGradering.getArbeidstidProsent());
            if (!gradering.getPeriode().getFomDato().isBefore(periode.getTomDato())) {
                break;
            }
            if (gradering.getPeriode().getTomDato().isBefore(nesteGradering.getPeriode().getFomDato()) &&
                !gradering.getPeriode().getTomDato().equals(nesteGradering.getPeriode().getFomDato().minusDays(1))) {
                leggTilNullProsent(prosentAndelerIPeriode);
            }
            gradering = nesteGradering;
        }

        if (periode.getTomDato() == null || gradering.getPeriode().getTomDato().isBefore(periode.getTomDato())) {
            leggTilNullProsent(prosentAndelerIPeriode);
        }

        return prosentAndelerIPeriode;
    }

    private void leggTilNullProsent(List<BigDecimal> prosentAndelerIPeriode) {
        if (!prosentAndelerIPeriode.contains(BigDecimal.ZERO)) {
            prosentAndelerIPeriode.add(BigDecimal.ZERO);
        }
    }

    private boolean graderingDekkerHeilePerioden(Gradering gradering, ÅpenDatoIntervallEntitet periode) {
        return !gradering.getPeriode().getFomDato().isAfter(periode.getFomDato()) &&
            (gradering.getPeriode().getTomDato().equals(TIDENES_ENDE) ||
                (periode.getTomDato() != null &&
                    !gradering.getPeriode().getTomDato().isBefore(periode.getTomDato())));
    }

}
