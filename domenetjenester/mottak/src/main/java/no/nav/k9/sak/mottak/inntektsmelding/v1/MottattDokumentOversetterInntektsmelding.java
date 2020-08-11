package no.nav.k9.sak.mottak.inntektsmelding.v1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;

import no.nav.inntektsmelding.xml.kodeliste._2018xxyy.NaturalytelseKodeliste;
import no.nav.inntektsmelding.xml.kodeliste._2018xxyy.ÅrsakInnsendingKodeliste;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsmeldingInnsendingsårsak;
import no.nav.k9.kodeverk.arbeidsforhold.NaturalYtelseType;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.NaturalYtelse;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.domene.iay.modell.UtsettelsePeriode;
import no.nav.k9.sak.mottak.inntektsmelding.InntektsmeldingFeil;
import no.nav.k9.sak.mottak.inntektsmelding.MottattInntektsmeldingOversetter;
import no.nav.k9.sak.mottak.inntektsmelding.NamespaceRef;
import no.nav.k9.sak.mottak.inntektsmelding.ValiderInntektsmelding;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;
import no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20180924.EndringIRefusjonsListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.NaturalytelseDetaljer;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Periode;

@NamespaceRef(InntektsmeldingConstants.NAMESPACE)
@ApplicationScoped
public class MottattDokumentOversetterInntektsmelding implements MottattInntektsmeldingOversetter<MottattDokumentWrapperInntektsmelding> {

    private static final LocalDate TIDENES_BEGYNNELSE = LocalDate.of(1, Month.JANUARY, 1);
    private static Map<ÅrsakInnsendingKodeliste, InntektsmeldingInnsendingsårsak> innsendingsårsakMap;

    static {
        innsendingsårsakMap = new EnumMap<>(ÅrsakInnsendingKodeliste.class);
        innsendingsårsakMap.put(ÅrsakInnsendingKodeliste.ENDRING, InntektsmeldingInnsendingsårsak.ENDRING);
        innsendingsårsakMap.put(ÅrsakInnsendingKodeliste.NY, InntektsmeldingInnsendingsårsak.NY);
    }

    private VirksomhetTjeneste virksomhetTjeneste;
    private ValiderInntektsmelding validator = new ValiderInntektsmelding();

    MottattDokumentOversetterInntektsmelding() {
        // for CDI proxy
    }

    @Inject
    public MottattDokumentOversetterInntektsmelding(VirksomhetTjeneste virksomhetTjeneste) {
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    @Override
    public InntektsmeldingBuilder trekkUtData(MottattDokumentWrapperInntektsmelding wrapper, MottattDokument mottattDokument) {
        String aarsakTilInnsending = wrapper.getSkjema().getSkjemainnhold().getAarsakTilInnsending();
        InntektsmeldingInnsendingsårsak innsendingsårsak = aarsakTilInnsending.isEmpty() ? InntektsmeldingInnsendingsårsak.UDEFINERT
            : innsendingsårsakMap.get(ÅrsakInnsendingKodeliste.fromValue(aarsakTilInnsending));

        InntektsmeldingBuilder builder = InntektsmeldingBuilder.builder();

        builder.medYtelse(wrapper.getYtelse());

        mapInnsendingstidspunkt(wrapper, mottattDokument, builder);
        builder.medMottattDato(mottattDokument.getMottattDato());
        builder.medKildesystem(wrapper.getAvsendersystem());
        builder.medKanalreferanse(mottattDokument.getKanalreferanse());
        builder.medJournalpostId(mottattDokument.getJournalpostId());

        mapArbeidsgiver(wrapper, builder);

        builder.medNærRelasjon(wrapper.getErNærRelasjon());
        builder.medInntektsmeldingaarsak(innsendingsårsak);

        mapArbeidsforholdOgBeløp(wrapper, builder);
        mapNaturalYtelser(wrapper, builder);
        mapFerie(wrapper, builder);
        mapRefusjon(wrapper, builder);
        
        builder.medOppgittFravær(validator.validerOppgittFravær(mottattDokument.getMottattDato(), wrapper.getOppgittFravær()));
        return builder;
    }
    
    private void mapArbeidsforholdOgBeløp(MottattDokumentWrapperInntektsmelding wrapper, InntektsmeldingBuilder builder) {
        final Optional<Arbeidsforhold> arbeidsforhold = wrapper.getArbeidsforhold();
        if (arbeidsforhold.isPresent()) {
            final Arbeidsforhold arbeidsforholdet = arbeidsforhold.get();
            final JAXBElement<String> arbeidsforholdId = arbeidsforholdet.getArbeidsforholdId();
            if (arbeidsforholdId != null) {
                var arbeidsforholdRef = EksternArbeidsforholdRef.ref(arbeidsforholdId.getValue());
                builder.medArbeidsforholdId(arbeidsforholdRef);
            }
            builder.medBeløp(validator.validerRefusjonMaks("arbeidsforhold.beregnetInntekt", arbeidsforholdet.getBeregnetInntekt().getValue().getBeloep().getValue()))
                .medStartDatoPermisjon(wrapper.getStartDatoPermisjon().orElse(null));
        } else {
            throw InntektsmeldingFeil.FACTORY.manglendeInformasjon().toException();
        }
    }

    private void mapArbeidsgiver(MottattDokumentWrapperInntektsmelding wrapper, InntektsmeldingBuilder builder) {
        String orgNummer = wrapper.getArbeidsgiver().getVirksomhetsnummer();
        @SuppressWarnings("unused")
        Virksomhet virksomhet = virksomhetTjeneste.hentOrganisasjon(orgNummer);
        builder.medArbeidsgiver(Arbeidsgiver.virksomhet(orgNummer));
    }

    private void mapInnsendingstidspunkt(MottattDokumentWrapperInntektsmelding wrapper, MottattDokument mottattDokument, InntektsmeldingBuilder builder) {
        if (wrapper.getInnsendingstidspunkt().isPresent()) { // LPS
            builder.medInnsendingstidspunkt(wrapper.getInnsendingstidspunkt().get());
        } else if (mottattDokument.getMottattTidspunkt() != null) { // Altinn
            builder.medInnsendingstidspunkt(mottattDokument.getMottattTidspunkt());
        } else {
            throw new IllegalArgumentException("Innsendingstidspunkt må være satt");
        }
    }

    private void mapRefusjon(MottattDokumentWrapperInntektsmelding wrapper, InntektsmeldingBuilder builder) {
        var optionalRefusjon = wrapper.getRefusjon();
        if (optionalRefusjon.isPresent()) {
            var refusjon = optionalRefusjon.get();
            BigDecimal refusjonsbeløp = refusjon.getRefusjonsbeloepPrMnd().getValue();
            if (refusjon.getRefusjonsopphoersdato() != null) {
                builder.medRefusjon(validator.validerRefusjonMaks("refusjon.refusjonsbeloepPrMnd", refusjonsbeløp), refusjon.getRefusjonsopphoersdato().getValue());
            } else if (refusjon.getRefusjonsbeloepPrMnd() != null) {
                builder.medRefusjon(refusjonsbeløp);
            }

            // Map endring i refusjon
            Optional.ofNullable(refusjon.getEndringIRefusjonListe())
                .map(JAXBElement::getValue)
                .map(EndringIRefusjonsListe::getEndringIRefusjon)
                .orElse(Collections.emptyList())
                .stream()
                .forEach(eir -> builder.leggTil(new Refusjon(validator.validerRefusjonEndringMaks("endringIRefusjon.refusjonsbeloepPrMnd", eir.getRefusjonsbeloepPrMnd().getValue(), eir.getEndringsdato().getValue()), eir.getEndringsdato().getValue())));

        }
    }
    
    private void mapFerie(MottattDokumentWrapperInntektsmelding wrapper, InntektsmeldingBuilder builder) {
        for (Periode periode : wrapper.getAvtaltFerie()) {
            builder.leggTil(UtsettelsePeriode.ferie(periode.getFom().getValue(), periode.getTom().getValue()));
        }
    }

    private void mapNaturalYtelser(MottattDokumentWrapperInntektsmelding wrapper, InntektsmeldingBuilder builder) {
        // Ved gjenopptakelse gjelder samme beløp
        Map<NaturalYtelseType, BigDecimal> beløp = new HashMap<>();
        for (NaturalytelseDetaljer detaljer : wrapper.getOpphørelseAvNaturalytelse()) {
            NaturalytelseKodeliste naturalytelse = NaturalytelseKodeliste.fromValue(detaljer.getNaturalytelseType().getValue());
            final NaturalYtelseType ytelseType = NaturalYtelseType.finnForKodeverkEiersKode(naturalytelse.value());
            beløp.put(ytelseType, detaljer.getBeloepPrMnd().getValue());
            LocalDate bortfallFom = detaljer.getFom().getValue();
            LocalDate naturalytelseTom = bortfallFom.minusDays(1);
            builder.leggTil(new NaturalYtelse(TIDENES_BEGYNNELSE, naturalytelseTom,
                beløp.get(ytelseType), ytelseType));
        }

        for (NaturalytelseDetaljer detaljer : wrapper.getGjenopptakelserAvNaturalytelse()) {
            NaturalytelseKodeliste naturalytelse = NaturalytelseKodeliste.fromValue(detaljer.getNaturalytelseType().getValue());
            final NaturalYtelseType ytelseType = NaturalYtelseType.finnForKodeverkEiersKode(naturalytelse.value());
            builder.leggTil(new NaturalYtelse(detaljer.getFom().getValue(), Tid.TIDENES_ENDE,
                beløp.get(ytelseType), ytelseType));
        }
    }
}
