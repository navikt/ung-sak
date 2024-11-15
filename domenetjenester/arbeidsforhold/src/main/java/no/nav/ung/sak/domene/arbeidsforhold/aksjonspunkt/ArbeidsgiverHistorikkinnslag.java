package no.nav.ung.sak.domene.arbeidsforhold.aksjonspunkt;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.ung.sak.domene.arbeidsforhold.impl.FinnNavnForManueltLagtTilArbeidsforholdTjeneste;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import no.nav.ung.sak.typer.OrgNummer;

@Dependent
public class ArbeidsgiverHistorikkinnslag {
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    ArbeidsgiverHistorikkinnslag() {
        // CDI
    }

    @Inject
    public ArbeidsgiverHistorikkinnslag(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
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

    public String lagHistorikkinnslagTekstForBeregningaktivitet(Arbeidsgiver arbeidsgiver,
                                                                InternArbeidsforholdRef arbeidsforholdRef,
                                                                OpptjeningAktivitetType opptjeningAktivitetType,
                                                                List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        if (arbeidsgiver != null) {
            return lagArbeidsgiverHistorikkinnslagTekst(arbeidsgiver, arbeidsforholdRef, arbeidsforholdOverstyringer);
        }
        return opptjeningAktivitetType.getNavn();
    }

    public String lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, List<ArbeidsforholdOverstyring> overstyringer) {
        if (arbeidsgiver != null && arbeidsforholdRef != null && arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            return lagTekstMedArbeidsgiverOgArbeidforholdRef(arbeidsgiver, arbeidsforholdRef, overstyringer);
        } else if (arbeidsgiver != null) {
            return lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer);
        }
        throw new IllegalStateException("Klarte ikke lage historikkinnslagstekst for arbeidsgiver");
    }

    public String lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        return lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer);
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

    private String lagTekstForArbeidsgiver(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        StringBuilder sb = new StringBuilder();
        String arbeidsgiverNavn = opplysninger.getNavn();
        if (arbeidsgiver.getErVirksomhet() && OrgNummer.erKunstig(arbeidsgiver.getOrgnr())) {
            arbeidsgiverNavn = hentNavnTilManueltArbeidsforhold(overstyringer);
        }
        sb.append(arbeidsgiverNavn)
            .append(" (")
            .append(opplysninger.getIdentifikator())
            .append(")");
        return sb.toString();
    }

    private String hentNavnTilManueltArbeidsforhold(List<ArbeidsforholdOverstyring> overstyringer) {
        return FinnNavnForManueltLagtTilArbeidsforholdTjeneste.finnNavnTilManueltLagtTilArbeidsforhold(overstyringer)
            .map(ArbeidsgiverOpplysninger::getNavn)
            .orElseThrow(() -> new IllegalStateException("Fant ikke forventet informasjon om manuelt arbeidsforhold"));
    }
}
