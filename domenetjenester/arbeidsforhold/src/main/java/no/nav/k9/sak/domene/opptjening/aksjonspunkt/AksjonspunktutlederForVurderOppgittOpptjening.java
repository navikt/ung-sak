package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static java.util.Collections.emptyList;
import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandling.aksjonspunkt.Utfall;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Inntektspost;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class AksjonspunktutlederForVurderOppgittOpptjening {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private static final Logger logger = LoggerFactory.getLogger(AksjonspunktutlederForVurderOppgittOpptjening.class);

    private OpptjeningRepository opptjeningRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;

    AksjonspunktutlederForVurderOppgittOpptjening() {
        // CDI
    }

    @Inject
    public AksjonspunktutlederForVurderOppgittOpptjening(OpptjeningRepository opptjeningRepository,
                                                         InntektArbeidYtelseTjeneste iayTjeneste,
                                                         VirksomhetTjeneste virksomhetTjeneste) {
        this.opptjeningRepository = opptjeningRepository;
        this.iayTjeneste = iayTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {

        Long behandlingId = param.getBehandlingId();

        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlagOptional = iayTjeneste.finnGrunnlag(behandlingId);
        var fastsattOpptjeningOptional = opptjeningRepository.finnOpptjening(behandlingId);
        if (inntektArbeidYtelseGrunnlagOptional.isEmpty() || fastsattOpptjeningOptional.isEmpty()) {
            return INGEN_AKSJONSPUNKTER;
        }
        OppgittOpptjening oppgittOpptjening = inntektArbeidYtelseGrunnlagOptional.get().getOppgittOpptjening().orElse(null);

        var opptjeningPerioder = fastsattOpptjeningOptional.get().getOpptjeningPerioder();

        for (Opptjening opptjening : opptjeningPerioder) {
            if (harBrukerOppgittPerioderMed(oppgittOpptjening, opptjening.getOpptjeningPeriode(), annenOpptjening()) == JA) {
                logger.info("Utleder AP 5051 fra oppgitt opptjening");
                return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
            }

            if (harBrukerOppgittArbeidsforholdMed(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD, opptjening.getOpptjeningPeriode(), oppgittOpptjening) == JA) {
                logger.info("Utleder AP 5051 fra utlandsk arbeidsforhold");
                return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
            }

            if (harBrukerOppgittÅVæreSelvstendigNæringsdrivende(oppgittOpptjening, opptjening.getOpptjeningPeriode()) == JA) {
                AktørId aktørId = param.getAktørId();
                if (manglerFerdiglignetNæringsinntekt(aktørId, oppgittOpptjening, inntektArbeidYtelseGrunnlagOptional.get(), opptjening.getOpptjeningPeriode()) == JA) {
                    logger.info("Utleder AP 5051 fra oppgitt næringsdrift");
                    return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
                }
            }
        }
        return INGEN_AKSJONSPUNKTER;
    }

    private List<ArbeidType> annenOpptjening() {
        return List.of(ArbeidType.values())
            .stream()
            .filter(ArbeidType::erAnnenOpptjening)
            .collect(Collectors.toList());
    }

    private Utfall harBrukerOppgittArbeidsforholdMed(ArbeidType annenOpptjeningType, DatoIntervallEntitet opptjeningPeriode,
                                                     OppgittOpptjening oppgittOpptjening) {
        if (oppgittOpptjening == null) {
            return NEI;
        }

        for (OppgittArbeidsforhold oppgittArbeidsforhold : oppgittOpptjening.getOppgittArbeidsforhold()) {
            if (oppgittArbeidsforhold.getArbeidType().equals(annenOpptjeningType) && opptjeningPeriode.overlapper(oppgittArbeidsforhold.getPeriode())) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall harBrukerOppgittPerioderMed(OppgittOpptjening oppgittOpptjening, DatoIntervallEntitet opptjeningPeriode,
                                               List<ArbeidType> annenOpptjeningType) {
        if (oppgittOpptjening == null) {
            return NEI;
        }

        for (OppgittAnnenAktivitet annenAktivitet : oppgittOpptjening.getAnnenAktivitet()) {
            if (annenOpptjeningType.contains(annenAktivitet.getArbeidType()) && opptjeningPeriode.overlapper(annenAktivitet.getPeriode())) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall harBrukerOppgittÅVæreSelvstendigNæringsdrivende(OppgittOpptjening oppgittOpptjening, DatoIntervallEntitet opptjeningPeriode) {
        if (oppgittOpptjening == null) {
            return NEI;
        }

        for (OppgittEgenNæring egenNæring : oppgittOpptjening.getEgenNæring()) {
            if (opptjeningPeriode.overlapper(egenNæring.getPeriode())) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall manglerFerdiglignetNæringsinntekt(AktørId aktørId, OppgittOpptjening oppgittOpptjening,
                                                     InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, DatoIntervallEntitet opptjeningPeriode) {
        // Det siste ferdiglignede år vil alltid være året før behandlingstidspunktet
        // Bruker LocalDate.now() her etter avklaring med funksjonell.
        int sistFerdiglignetÅr = LocalDate.now().minusYears(1L).getYear();
        if (inneholderSisteFerdiglignendeÅrNæringsinntekt(aktørId, inntektArbeidYtelseGrunnlag, sistFerdiglignetÅr, opptjeningPeriode) == NEI) {
            if (erDetRegistrertNæringEtterSisteFerdiglignendeÅr(oppgittOpptjening, sistFerdiglignetÅr) == NEI) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall inneholderSisteFerdiglignendeÅrNæringsinntekt(AktørId aktørId,
                                                                 InntektArbeidYtelseGrunnlag grunnlag,
                                                                 int sistFerdiglignetÅr,
                                                                 DatoIntervallEntitet opptjeningPeriode) {
        var filter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId));
        if (filter.isEmpty()) {
            return NEI;
        }

        var stpFilter = (opptjeningPeriode.getTomDato().getYear() >= sistFerdiglignetÅr) ? filter.før(opptjeningPeriode.getTomDato()) : filter.etter(opptjeningPeriode.getTomDato());

        return stpFilter.filterBeregnetSkatt().filter(InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE, InntektspostType.NÆRING_FISKE_FANGST_FAMBARNEHAGE)
            .anyMatchFilter(harInntektI(sistFerdiglignetÅr)) ? JA : NEI;
    }

    private BiPredicate<Inntekt, Inntektspost> harInntektI(int sistFerdiglignetÅr) {
        return (inntekt, inntektspost) -> inntektspost.getPeriode().getTomDato().getYear() == sistFerdiglignetÅr &&
            inntektspost.getBeløp().getVerdi().compareTo(BigDecimal.ZERO) != 0;
    }

    private Utfall erDetRegistrertNæringEtterSisteFerdiglignendeÅr(OppgittOpptjening oppgittOpptjening, int sistFerdiglignetÅr) {
        if (oppgittOpptjening == null) {
            return NEI;
        }

        return oppgittOpptjening.getEgenNæring().stream()
            .anyMatch(egenNæring -> erRegistrertNæring(egenNæring, sistFerdiglignetÅr)) ? JA : NEI;
    }

    private boolean erRegistrertNæring(OppgittEgenNæring eg, int sistFerdiglignetÅr) {
        if (eg.getOrgnr() == null) {
            return false;
        }
        Optional<Virksomhet> lagretVirksomhet = virksomhetTjeneste.finnOrganisasjon(eg.getOrgnr());
        if (lagretVirksomhet.isPresent()) {
            return lagretVirksomhet.get().getRegistrert().getYear() > sistFerdiglignetÅr;
        } else {
            // Virksomhetsinformasjonen er ikke hentet, henter vi den fra ereg. Innført etter feil som oppstod i https://jira.adeo.no/browse/TFP-1484
            Virksomhet hentetVirksomhet = virksomhetTjeneste.hentOrganisasjon(eg.getOrgnr());
            return hentetVirksomhet.getRegistrert().getYear() > sistFerdiglignetÅr;
        }
    }

    boolean girAksjonspunktForOppgittNæring(AktørId aktørId, InntektArbeidYtelseGrunnlag iayg, DatoIntervallEntitet opptjeningPeriode) {
        if (opptjeningPeriode == null) {
            return false;
        }
        OppgittOpptjening oppgittOpptjening = iayg.getOppgittOpptjening().orElse(null);

        return harBrukerOppgittÅVæreSelvstendigNæringsdrivende(oppgittOpptjening, opptjeningPeriode) == JA &&
            manglerFerdiglignetNæringsinntekt(aktørId, oppgittOpptjening, iayg, opptjeningPeriode) == JA;
    }
}
