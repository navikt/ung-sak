package no.nav.k9.sak.domene.arbeidsforhold;

import static java.util.Collections.emptyList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingSomIkkeKommer;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class InntektsmeldingTjeneste {

    private final InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public InntektsmeldingTjeneste(InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
    }

    /**
     * Filtrer vekk inntektsmeldinger som er knyttet til et arbeidsforhold som har en tom dato som slutter før STP.
     */
    private static List<Inntektsmelding> filtrerVekkInntektsmeldingPåInaktiveArbeidsforhold(YrkesaktivitetFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                                            Collection<Inntektsmelding> inntektsmeldinger,
                                                                                            LocalDate skjæringstidspunktet) {
        List<Inntektsmelding> resultat = new ArrayList<>();

        inntektsmeldinger.forEach(im -> {
            boolean skalLeggeTil = yrkesaktiviteter.stream()
                .anyMatch(y -> {
                    boolean gjelderFor = y.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef());
                    var ansettelsesPerioder = filter.getAnsettelsesPerioder(y);
                    return gjelderFor && ansettelsesPerioder.stream()
                        .anyMatch(ap -> ap.getPeriode().inkluderer(skjæringstidspunktet) || ap.getPeriode().getFomDato().isAfter(skjæringstidspunktet));
                });
            if (skalLeggeTil) {
                resultat.add(im);
            }
        });
        return Collections.unmodifiableList(resultat);
    }

    /**
     * Henter alle inntektsmeldinger
     * Tar hensyn til inaktive arbeidsforhold, dvs. fjerner de
     * inntektsmeldingene som er koblet til inaktivte arbeidsforhold
     *
     * @param ref                             {@link BehandlingReferanse}
     * @param skjæringstidspunktForOpptjening datoen arbeidsforhold må inkludere eller starte etter for å bli regnet som aktive
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentInntektsmeldinger(BehandlingReferanse ref, LocalDate skjæringstidspunktForOpptjening) {
        Long behandlingId = ref.getBehandlingId();
        AktørId aktørId = ref.getAktørId();
        return hentInntektsmeldinger(behandlingId, aktørId, skjæringstidspunktForOpptjening);
    }

    public List<Inntektsmelding> hentInntektsmeldinger(Long behandlingId, AktørId aktørId, LocalDate skjæringstidspunktForOpptjening) {
        return iayTjeneste.finnGrunnlag(behandlingId).map(g -> hentInntektsmeldinger(aktørId, skjæringstidspunktForOpptjening, g)).orElse(Collections.emptyList());
    }

    public List<Inntektsmelding> hentInntektsmeldinger(AktørId aktørId, LocalDate skjæringstidspunktForOpptjening, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<Inntektsmelding> inntektsmeldinger = iayGrunnlag.getInntektsmeldinger().map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(emptyList());

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        // kan ikke filtrere når det ikke finnes yrkesaktiviteter
        if (yrkesaktiviteter.isEmpty()) {
            return inntektsmeldinger;
        }
        return filtrerVekkInntektsmeldingPåInaktiveArbeidsforhold(filter, yrkesaktiviteter, inntektsmeldinger, skjæringstidspunktForOpptjening);
    }

    /**
     * Henter ut alle inntektsmeldinger mottatt etter gjeldende vedtak
     * Denne metoden benyttes <b>BARE</b> for revurderinger
     *
     * @param ref referanse til behandlingen
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentAlleInntektsmeldingerMottattEtterGjeldendeVedtak(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        Long originalBehandlingId = ref.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Denne metoden benyttes bare for revurderinger"));

        Map<String, Inntektsmelding> revurderingIM = hentIMMedIndexKey(behandlingId);
        Map<String, Inntektsmelding> origIM = hentIMMedIndexKey(originalBehandlingId);
        return revurderingIM.entrySet().stream()
            .filter(imRevurderingEntry -> !origIM.containsKey(imRevurderingEntry.getKey())
                || !Objects.equals(origIM.get(imRevurderingEntry.getKey()).getJournalpostId(), imRevurderingEntry.getValue().getJournalpostId()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    public Optional<Inntektsmelding> hentInntektsMeldingFor(Long behandlingId, JournalpostId journalpostId) {
        var grunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        return grunnlag.getInntektsmeldinger().stream().flatMap(imagg -> imagg.getAlleInntektsmeldinger().stream())
            .filter(im -> Objects.equals(im.getJournalpostId(), journalpostId)).findFirst();
    }

    /**
     * Henter kombinasjon av arbeidsgiver + arbeidsforholdRef
     * på de det ikke vil komme inn inntektsmelding for.
     *
     * @param behandlingId iden til behandlingen
     * @return Liste med inntektsmelding som ikke kommer {@link InntektsmeldingSomIkkeKommer}
     */
    public List<InntektsmeldingSomIkkeKommer> hentAlleInntektsmeldingerSomIkkeKommer(Long behandlingId) {
        List<InntektsmeldingSomIkkeKommer> result = new ArrayList<>();
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag = iayTjeneste.finnGrunnlag(behandlingId);
        inntektArbeidYtelseGrunnlag.ifPresent(iayg -> result.addAll(iayg.getInntektsmeldingerSomIkkeKommer()));
        return result;
    }

    /**
     * Henter ut alle inntektsmeldinger koblet til angitte behandlinger
     * <br>
     * <b>NB!</b> Tar ikke hensyn til om inntektsmeldingen er knyttet til et inaktivt arbeidsforhold
     *
     * @param behandlingIder behandlingene
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentAlleInntektsmeldingerForAngitteBehandlinger(Set<Long> behandlingIder) {
        return hentUtAlleInntektsmeldingeneFraBehandlingene(behandlingIder);
    }

    private Map<String, Inntektsmelding> hentIMMedIndexKey(Long behandlingId) {
        List<Inntektsmelding> inntektsmeldinger = iayTjeneste.finnGrunnlag(behandlingId)
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(Collections.emptyList());

        return inntektsmeldinger.stream()
            .collect(Collectors.toMap(Inntektsmelding::getIndexKey, im -> im));
    }

    private List<Inntektsmelding> hentUtAlleInntektsmeldingeneFraBehandlingene(Collection<Long> behandlingIder) {
        // FIXME (FC) denne burde gått rett på datalagret istd. å iterere over åpne behandlinger
        List<Inntektsmelding> inntektsmeldinger = new ArrayList<>();
        for (Long behandlingId : behandlingIder) {
            inntektsmeldinger.addAll(hentAlleInntektsmeldinger(behandlingId));
        }
        return inntektsmeldinger;
    }

    private List<Inntektsmelding> hentAlleInntektsmeldinger(Long behandlingId) {
        return iayTjeneste.finnGrunnlag(behandlingId)
            .map(iayGrunnlag -> iayGrunnlag.getInntektsmeldinger()
                .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes).orElse(emptyList()))
            .orElse(emptyList());
    }

    public void lagreInntektsmeldinger(Saksnummer saksnummer, Long behandlingId, List<InntektsmeldingBuilder> inntektsmeldinger) {
        iayTjeneste.lagreInntektsmeldinger(saksnummer, behandlingId, inntektsmeldinger);
    }
}
