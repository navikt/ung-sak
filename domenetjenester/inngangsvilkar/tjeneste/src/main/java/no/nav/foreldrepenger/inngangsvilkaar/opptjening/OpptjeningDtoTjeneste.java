package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.MergeOverlappendePeriodeHjelp;
import no.nav.k9.sak.domene.arbeidsforhold.impl.FinnNavnForManueltLagtTilArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.kontrakt.opptjening.FastsattOpptjeningDto;
import no.nav.k9.sak.kontrakt.opptjening.OpptjeningAktivitetDto;
import no.nav.k9.sak.kontrakt.opptjening.OpptjeningDto;
import no.nav.k9.sak.kontrakt.opptjening.OpptjeningPeriodeDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.OrganisasjonsNummerValidator;
import no.nav.k9.sak.typer.Stillingsprosent;

@ApplicationScoped
public class OpptjeningDtoTjeneste {
    private OpptjeningsperioderTjeneste forSaksbehandlingTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    OpptjeningDtoTjeneste() {
        // Hibernate
    }

    @Inject
    public OpptjeningDtoTjeneste(OpptjeningsperioderTjeneste forSaksbehandlingTjeneste,
                                     ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                     InntektArbeidYtelseTjeneste iayTjeneste) {
        this.forSaksbehandlingTjeneste = forSaksbehandlingTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.iayTjeneste = iayTjeneste;
    }

    public Optional<OpptjeningDto> mapFra(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        Optional<Opptjening> fastsattOpptjening = forSaksbehandlingTjeneste.hentOpptjeningHvisFinnes(behandlingId);

        OpptjeningDto resultat = new OpptjeningDto();
        if (fastsattOpptjening.isPresent() && fastsattOpptjening.get().getAktiv()) {
            List<OpptjeningAktivitet> opptjeningAktivitet = fastsattOpptjening.get().getOpptjeningAktivitet();
            resultat.setFastsattOpptjening(new FastsattOpptjeningDto(fastsattOpptjening.get().getFom(),
                fastsattOpptjening.get().getTom(), mapFastsattOpptjening(fastsattOpptjening.get()),
                MergeOverlappendePeriodeHjelp.mergeOverlappenePerioder(opptjeningAktivitet)));
        }
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlagOpt = iayTjeneste.finnGrunnlag(behandlingId);
        List<ArbeidsforholdOverstyring> overstyringer = inntektArbeidYtelseGrunnlagOpt.map(InntektArbeidYtelseGrunnlag::getArbeidsforholdOverstyringer).orElse(Collections.emptyList());

        if (fastsattOpptjening.isPresent()) {
            resultat.setOpptjeningAktivitetList(forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, inntektArbeidYtelseGrunnlagOpt)
                .stream()
                .map(oap -> lagDtoFraOAPeriode(oap, overstyringer))
                .collect(Collectors.toList()));
        } else {
            resultat.setOpptjeningAktivitetList(Collections.emptyList());
        }

        if (resultat.getFastsattOpptjening() == null && resultat.getOpptjeningAktivitetList().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(resultat);
    }

    private OpptjeningPeriodeDto mapFastsattOpptjening(Opptjening fastsattOpptjening) {
        return fastsattOpptjening.getOpptjentPeriode() != null ? new OpptjeningPeriodeDto(fastsattOpptjening.getOpptjentPeriode().getMonths(),
            fastsattOpptjening.getOpptjentPeriode().getDays()) : new OpptjeningPeriodeDto();
    }

    private OpptjeningAktivitetDto lagDtoFraOAPeriode(OpptjeningsperiodeForSaksbehandling oap, List<ArbeidsforholdOverstyring> overstyringer) {
        var dto = new OpptjeningAktivitetDto(oap.getOpptjeningAktivitetType(),
            oap.getPeriode().getFomDato(), oap.getPeriode().getTomDato());

        var arbeidsgiver = oap.getArbeidsgiver();
        if (arbeidsgiver != null && arbeidsgiver.erAktørId()) {
            lagOpptjeningAktivitetDtoForPrivatArbeidsgiver(oap, dto);
        } else if (arbeidsgiver != null && OrganisasjonsNummerValidator.erGyldig(arbeidsgiver.getOrgnr())) {
            lagOpptjeningAktivitetDtoForArbeidsgiver(oap, dto, false, overstyringer);
        } else if (erKunstig(oap)) {
            lagOpptjeningAktivitetDtoForArbeidsgiver(oap, dto, true, overstyringer);
        } else {
            dto.setArbeidsgiver(oap.getArbeidsgiverUtlandNavn());
        }
        settVurdering(oap, dto);
        leggPåFellesEgenskaper(oap, dto);
        return dto;
    }

    private void settVurdering(OpptjeningsperiodeForSaksbehandling oap, OpptjeningAktivitetDto dto) {
        if (oap.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)) {
            dto.setErGodkjent(true);
        } else if (oap.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)) {
            dto.setErGodkjent(false);
        }
    }

    private void leggPåFellesEgenskaper(OpptjeningsperiodeForSaksbehandling oap, OpptjeningAktivitetDto dto) {
        dto.setErManueltOpprettet(oap.getErManueltRegistrert());
        dto.setBegrunnelse(oap.getBegrunnelse());
        dto.setErEndret(oap.erManueltBehandlet());
        dto.setErPeriodeEndret(oap.getErPeriodeEndret());
        dto.setArbeidsforholdRef(Optional.ofNullable(oap.getOpptjeningsnøkkel())
            .flatMap(Opptjeningsnøkkel::getArbeidsforholdRef)
            .map(InternArbeidsforholdRef::getReferanse)
            .orElse(null));
    }

    private void lagOpptjeningAktivitetDtoForArbeidsgiver(OpptjeningsperiodeForSaksbehandling oap, OpptjeningAktivitetDto dto, boolean kunstig, List<ArbeidsforholdOverstyring> overstyringer) {
        if (kunstig) {
            hentNavnTilManueltArbeidsforhold(overstyringer).ifPresent(a -> dto.setArbeidsgiver(a.getNavn()));
        } else {
            Arbeidsgiver arbeidsgiver = oap.getArbeidsgiver();
            if (arbeidsgiver != null) {
                var virksomhet = arbeidsgiverTjeneste.hentVirksomhet(arbeidsgiver.getOrgnr());
                dto.setArbeidsgiver(virksomhet.getNavn());
                dto.setNaringRegistreringsdato(virksomhet.getRegistrert());
            }
        }
        dto.setOppdragsgiverOrg(oap.getOrgnr());
        dto.setArbeidsgiverIdentifikator(oap.getOrgnr());
        dto.setStillingsandel(Optional.ofNullable(oap.getStillingsprosent()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO));
    }

    private void lagOpptjeningAktivitetDtoForPrivatArbeidsgiver(OpptjeningsperiodeForSaksbehandling oap, OpptjeningAktivitetDto dto) {
        ArbeidsgiverOpplysninger arbeidsgiver = arbeidsgiverTjeneste.hent(oap.getArbeidsgiver());
        if (arbeidsgiver != null) {
            dto.setPrivatpersonNavn(arbeidsgiver.getNavn());
            dto.setPrivatpersonFødselsdato(arbeidsgiver.getFødselsdato());
            dto.setArbeidsgiver(arbeidsgiver.getNavn());
        }
        dto.setOppdragsgiverOrg(oap.getArbeidsgiver().getIdentifikator());
        dto.setArbeidsgiverIdentifikator(oap.getArbeidsgiver().getIdentifikator());
        dto.setStillingsandel(Optional.ofNullable(oap.getStillingsprosent()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO));
    }

    private boolean erKunstig(OpptjeningsperiodeForSaksbehandling oap) {
        Arbeidsgiver arbeidsgiver = oap.getArbeidsgiver();
        if (arbeidsgiver != null && arbeidsgiver.getErVirksomhet()) {
            return OrgNummer.erKunstig(arbeidsgiver.getOrgnr());
        }
        return false;
    }

     private Optional<ArbeidsgiverOpplysninger> hentNavnTilManueltArbeidsforhold(List<ArbeidsforholdOverstyring> overstyringer) {
         return FinnNavnForManueltLagtTilArbeidsforholdTjeneste.finnNavnTilManueltLagtTilArbeidsforhold(overstyringer);
    }

}
