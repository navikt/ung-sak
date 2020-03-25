package no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding.v2;

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
import no.nav.inntektsmelding.xml.kodeliste._2018xxyy.ÅrsakUtsettelseKodeliste;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsmeldingInnsendingsårsak;
import no.nav.k9.kodeverk.arbeidsforhold.NaturalYtelseType;
import no.nav.k9.kodeverk.arbeidsforhold.UtsettelseÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.Gradering;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.NaturalYtelse;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.domene.iay.modell.UtsettelsePeriode;
import no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding.InntektsmeldingFeil;
import no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding.InntektsmeldingInnhold;
import no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding.MottattInntektsmeldingOversetter;
import no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding.NamespaceRef;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumer;
import no.nav.vedtak.konfig.Tid;
import no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20181211.EndringIRefusjonsListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.GraderingIForeldrepenger;
import no.seres.xsd.nav.inntektsmelding_m._20181211.NaturalytelseDetaljer;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Periode;
import no.seres.xsd.nav.inntektsmelding_m._20181211.UtsettelseAvForeldrepenger;

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
    private AktørConsumer aktørConsumer;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    MottattDokumentOversetterInntektsmelding() {
        // for CDI proxy
    }

    @Inject
    public MottattDokumentOversetterInntektsmelding(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                                    VirksomhetTjeneste virksomhetTjeneste,
                                                    AktørConsumer aktørConsumer) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.aktørConsumer = aktørConsumer;
    }

    @Override
    public InntektsmeldingInnhold trekkUtDataOgPersister(MottattDokumentWrapperInntektsmelding wrapper, MottattDokument mottattDokument, Behandling behandling, Optional<LocalDate> gjelderFra) {
        String aarsakTilInnsending = wrapper.getSkjema().getSkjemainnhold().getAarsakTilInnsending();
        InntektsmeldingInnsendingsårsak innsendingsårsak = aarsakTilInnsending.isEmpty() ?
            InntektsmeldingInnsendingsårsak.UDEFINERT :
            innsendingsårsakMap.get(ÅrsakInnsendingKodeliste.fromValue(aarsakTilInnsending));

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
        mapGradering(wrapper, builder);
        mapFerie(wrapper, builder);
        mapUtsettelse(wrapper, builder);
        mapRefusjon(wrapper, builder);
        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(), builder);
        
        return new InntektsmeldingInnhold(builder.build(), wrapper.getOmsorgspenger());
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
            builder.medBeløp(arbeidsforholdet.getBeregnetInntekt().getValue().getBeloep().getValue())
                .medStartDatoPermisjon(wrapper.getStartDatoPermisjon().orElse(null));
        } else {
            throw InntektsmeldingFeil.FACTORY.manglendeInformasjon().toException();
        }
    }

    private void mapArbeidsgiver(MottattDokumentWrapperInntektsmelding wrapper, InntektsmeldingBuilder builder) {
        if (wrapper.getArbeidsgiver().isPresent()) {
            String orgNummer = wrapper.getArbeidsgiver().get().getVirksomhetsnummer();
            @SuppressWarnings("unused")
            Virksomhet virksomhet = virksomhetTjeneste.hentOgLagreOrganisasjon(orgNummer);
            builder.medArbeidsgiver(Arbeidsgiver.virksomhet(orgNummer));
        } else if (wrapper.getArbeidsgiverPrivat().isPresent()) {
            Optional<String> aktørIdArbeidsgiver = aktørConsumer.hentAktørIdForPersonIdent(wrapper.getArbeidsgiverPrivat().get().getArbeidsgiverFnr());
            if (!aktørIdArbeidsgiver.isPresent()) {
                throw InntektsmeldingFeil.FACTORY.finnerIkkeArbeidsgiverITPS().toException();
            }
            builder.medArbeidsgiver(Arbeidsgiver.person(new AktørId(aktørIdArbeidsgiver.get())));
        } else {
            throw InntektsmeldingFeil.FACTORY.manglendeArbeidsgiver().toException();
        }
    }

    private void mapInnsendingstidspunkt(MottattDokumentWrapperInntektsmelding wrapper, MottattDokument mottattDokument, InntektsmeldingBuilder builder) {
        if (wrapper.getInnsendingstidspunkt().isPresent()) { // LPS
            builder.medInnsendingstidspunkt(wrapper.getInnsendingstidspunkt().get());
        } else if (mottattDokument.getMottattTidspunkt() != null) { // Altinn
            builder.medInnsendingstidspunkt(mottattDokument.getMottattTidspunkt());
        } else {
            builder.medInnsendingstidspunkt(LocalDateTime.now());
        }
    }

    private void mapRefusjon(MottattDokumentWrapperInntektsmelding wrapper, InntektsmeldingBuilder builder) {
        var optionalRefusjon = wrapper.getRefusjon();
        if (optionalRefusjon.isPresent()) {
            var refusjon = optionalRefusjon.get();
            if (refusjon.getRefusjonsopphoersdato() != null) {
                builder.medRefusjon(refusjon.getRefusjonsbeloepPrMnd().getValue(),
                    refusjon.getRefusjonsopphoersdato().getValue());
            } else if (refusjon.getRefusjonsbeloepPrMnd() != null) {
                builder.medRefusjon(refusjon.getRefusjonsbeloepPrMnd().getValue());
            }

            // Map endring i refusjon
            Optional.ofNullable(refusjon.getEndringIRefusjonListe())
                .map(JAXBElement::getValue)
                .map(EndringIRefusjonsListe::getEndringIRefusjon)
                .orElse(Collections.emptyList())
                .stream()
                .forEach(eir -> builder.leggTil(new Refusjon(eir.getRefusjonsbeloepPrMnd().getValue(), eir.getEndringsdato().getValue())));

        }
    }

    private void mapUtsettelse(MottattDokumentWrapperInntektsmelding wrapper, InntektsmeldingBuilder builder) {
        for (UtsettelseAvForeldrepenger detaljer : wrapper.getUtsettelser()) {
            // FIXME (weak reference)
            ÅrsakUtsettelseKodeliste årsakUtsettelse = ÅrsakUtsettelseKodeliste.fromValue(detaljer.getAarsakTilUtsettelse().getValue());
            final UtsettelseÅrsak årsak = UtsettelseÅrsak.fraKode(årsakUtsettelse.name());
            builder.leggTil(UtsettelsePeriode.utsettelse(detaljer.getPeriode().getValue().getFom().getValue(),
                detaljer.getPeriode().getValue().getTom().getValue(), årsak));
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

    private void mapGradering(MottattDokumentWrapperInntektsmelding wrapper, InntektsmeldingBuilder builder) {
        for (GraderingIForeldrepenger detaljer : wrapper.getGradering()) {
            builder.leggTil(new Gradering(detaljer.getPeriode().getValue().getFom().getValue(),
                detaljer.getPeriode().getValue().getTom().getValue(),
                new BigDecimal(detaljer.getArbeidstidprosent().getValue())));
        }
    }
}
