package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;

@ApplicationScoped
public class ArbeidsgiverHistorikkinnslag {
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    ArbeidsgiverHistorikkinnslag() {
        // CDI
    }

    @Inject
    public ArbeidsgiverHistorikkinnslag(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    private String lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, List<ArbeidsforholdOverstyring> overstyringer) {
        if (arbeidsgiver != null && arbeidsforholdRef != null && arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            return lagTekstMedArbeidsgiverOgArbeidforholdRef(arbeidsgiver, arbeidsforholdRef, overstyringer);
        } else if (arbeidsgiver != null) {
            return lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer);
        }
        throw new IllegalStateException("Klarte ikke lage historikkinnslagstekst for arbeidsgiver");
    }

    private String lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        return lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer);
    }

    public String lagHistorikkinnslagTekstForBeregningsgrunnlag(AktivitetStatus aktivitetStatus,
                                                                Optional<Arbeidsgiver> arbeidsgiver,
                                                                Optional<InternArbeidsforholdRef> arbeidsforholdRef,
                                                                List<ArbeidsforholdOverstyring> overstyringer) {
        return arbeidsgiver.map(arbGiv -> arbeidsforholdRef.isPresent() && arbeidsforholdRef.get().gjelderForSpesifiktArbeidsforhold()
            ? lagArbeidsgiverHistorikkinnslagTekst(arbGiv, arbeidsforholdRef.get(), overstyringer)
            : lagArbeidsgiverHistorikkinnslagTekst(arbGiv, overstyringer))
            .orElse(aktivitetStatus.getNavn());
    }

    public String lagHistorikkinnslagTekstForBeregningaktivitet(BeregningAktivitetEntitet beregningAktivitet, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        Arbeidsgiver arbeidsgiver = beregningAktivitet.getArbeidsgiver();
        InternArbeidsforholdRef arbeidsforholdRef = beregningAktivitet.getArbeidsforholdRef();
        if (arbeidsgiver != null) {
            return lagArbeidsgiverHistorikkinnslagTekst(arbeidsgiver, arbeidsforholdRef, arbeidsforholdOverstyringer);
        }
        return beregningAktivitet.getOpptjeningAktivitetType().getNavn();
    }

    private String lagTekstMedArbeidsgiver(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
        return lagTekstForArbeidsgiver(arbeidsgiver, overstyringer);
    }

    private String lagTekstMedArbeidsgiverOgArbeidforholdRef(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, List<ArbeidsforholdOverstyring> overstyringer) {
        StringBuilder sb = new StringBuilder();
        sb.append(lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer));
        sb.append(lagTekstMedArbeidsforholdref(arbeidsforholdRef));
        return sb.toString();
    }

    private String lagTekstMedArbeidsforholdref(InternArbeidsforholdRef arbeidsforholdRef) {
        String referanse = arbeidsforholdRef.getReferanse();
        String sisteFireTegnIRef = referanse.substring(referanse.length() - 4);
        StringBuilder sb = new StringBuilder();
        sb.append(" ...")
            .append(sisteFireTegnIRef);
        return sb.toString();

    }

    public String lagTekstForArbeidsgiver(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> arbeidsforholOverstyringer) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        StringBuilder sb = new StringBuilder();
        String arbeidsgiverNavn = opplysninger.getNavn();
        if (arbeidsgiver.getErVirksomhet() && OrgNummer.erKunstig(arbeidsgiver.getOrgnr())) {
            arbeidsgiverNavn = hentNavnTilManueltArbeidsforhold(arbeidsforholOverstyringer);
        }
        sb.append(arbeidsgiverNavn)
            .append(" (")
            .append(opplysninger.getIdentifikator())
            .append(")");
        return sb.toString();
    }

    private String hentNavnTilManueltArbeidsforhold(List<ArbeidsforholdOverstyring> arbeidsforholOverstyringer) {
        return arbeidsforholOverstyringer
            .stream()
            .findFirst()
            .map(ArbeidsforholdOverstyring::getArbeidsgiverNavn)
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Kaller denne uten overstyring "));
    }
}
