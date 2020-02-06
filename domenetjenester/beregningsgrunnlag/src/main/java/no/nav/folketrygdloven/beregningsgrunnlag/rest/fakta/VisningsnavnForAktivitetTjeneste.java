package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.FinnNavnForManueltLagtTilArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;

@ApplicationScoped
public class VisningsnavnForAktivitetTjeneste {

    private static final int ANTALL_SIFFER_SOM_SKAL_VISES_AV_REFERANSE = 4;

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    public VisningsnavnForAktivitetTjeneste() {
        // For CDI
    }

    @Inject
    public VisningsnavnForAktivitetTjeneste(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    String lagVisningsnavn(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, BeregningsgrunnlagPrStatusOgAndel andel) {
        if (andel.getAktivitetStatus().erArbeidstaker()) {
            return finnVisningsnavnForArbeidstaker(ref, iayGrunnlag, andel);
        }
        return andel.getArbeidsforholdType() == null || OpptjeningAktivitetType.UDEFINERT.equals(andel.getArbeidsforholdType()) ? andel.getAktivitetStatus().getNavn() : andel.getArbeidsforholdType().getNavn();
    }

    private String finnVisningsnavnForArbeidstaker(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, BeregningsgrunnlagPrStatusOgAndel andel) {
        return andel.getBgAndelArbeidsforhold()
            .map(bgAndelArbeidsforhold -> {
                Arbeidsgiver arbeidsgiver = bgAndelArbeidsforhold.getArbeidsgiver();
                String visningsnavnUtenReferanse = finnVisningsnavnUtenReferanse(arbeidsgiver, iayGrunnlag);
                return finnVisningsnavnMedReferanseHvisFinnes(ref, arbeidsgiver, bgAndelArbeidsforhold, visningsnavnUtenReferanse, iayGrunnlag);
            }).orElse(andel.getArbeidsforholdType().getNavn());
    }

    private String finnVisningsnavnMedReferanseHvisFinnes(BehandlingReferanse ref, Arbeidsgiver arbeidsgiver, BGAndelArbeidsforhold bgAndelArbeidsforhold, String visningsnavnUtenReferanse, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        String referanse = bgAndelArbeidsforhold.getArbeidsforholdRef().getReferanse();
        if (referanse != null) {
            if (inntektArbeidYtelseGrunnlag.getArbeidsforholdInformasjon().isEmpty()) {
                throw new IllegalStateException("Mangler arbeidsforholdinformasjon for behandlingId=" + ref.getBehandlingId());
            }
            var eksternArbeidsforholdRef = inntektArbeidYtelseGrunnlag.getArbeidsforholdInformasjon().get().finnEkstern(arbeidsgiver, bgAndelArbeidsforhold.getArbeidsforholdRef());
            var eksternArbeidsforholdId = eksternArbeidsforholdRef.getReferanse();
            return visningsnavnUtenReferanse + " ..." + finnSubstringAvReferanse(eksternArbeidsforholdId);
        }
        return visningsnavnUtenReferanse;
    }

    private String finnSubstringAvReferanse(String eksternArbeidsforholdId) {
        if (eksternArbeidsforholdId == null) {
            return "";
        }
        if (eksternArbeidsforholdId.length() <= ANTALL_SIFFER_SOM_SKAL_VISES_AV_REFERANSE) {
            return eksternArbeidsforholdId;
        }
        return eksternArbeidsforholdId.substring(eksternArbeidsforholdId.length() - ANTALL_SIFFER_SOM_SKAL_VISES_AV_REFERANSE);
    }

    private String finnVisningsnavnUtenReferanse(Arbeidsgiver arbeidsgiver,
                                                 InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        String arbeidsgiverNavn = null;
        String arbeidsgiverId = null;
        if (arbeidsgiver.getErVirksomhet()) {
            if (OrgNummer.erKunstig(arbeidsgiver.getOrgnr())) {
                arbeidsgiverNavn = hentNavnTilManueltArbeidsforhold(inntektArbeidYtelseGrunnlag);
                arbeidsgiverId = arbeidsgiver.getOrgnr();
            } else {
                Virksomhet virksomhet = arbeidsgiverTjeneste.hentVirksomhet(arbeidsgiver.getOrgnr());
                arbeidsgiverId = virksomhet.getOrgnr();
                arbeidsgiverNavn = virksomhet.getNavn();
            }
        } else if (arbeidsgiver.erAktørId()) {
            ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            arbeidsgiverNavn = opplysninger.getNavn();
            arbeidsgiverId = opplysninger.getIdentifikator();
        }
        return arbeidsgiverNavn + " (" + arbeidsgiverId + ")";
    }

    private String hentNavnTilManueltArbeidsforhold(InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        List<ArbeidsforholdOverstyring> overstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        return FinnNavnForManueltLagtTilArbeidsforholdTjeneste.finnNavnTilManueltLagtTilArbeidsforhold(overstyringer)
            .map(ArbeidsgiverOpplysninger::getNavn)
            .orElseThrow(() -> new IllegalStateException("Fant ikke forventet informasjon om manuelt arbeidsforhold"));
    }
}
