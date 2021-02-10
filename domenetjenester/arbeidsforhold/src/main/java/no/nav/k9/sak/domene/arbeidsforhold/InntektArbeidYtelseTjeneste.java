package no.nav.k9.sak.domene.arbeidsforhold;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

public interface InntektArbeidYtelseTjeneste {
    /**
     * Hent grunnlag.
     *
     * @param behandlingId
     * @return henter aggregat, kaster feil hvis det ikke finnes.
     */
    InntektArbeidYtelseGrunnlag hentGrunnlag(Long behandlingId);

    /**
     * Hent grunnlag.
     *
     * @param behandlingUuid
     * @return henter aggregat, kaster feil hvis det ikke finnes.
     */
    InntektArbeidYtelseGrunnlag hentGrunnlag(UUID behandlingUuid);

    /**
     * Hent grunnlag gitt grunnlag id
     *
     * @param behandlingId
     * @param grunnlagUuid
     */
    InntektArbeidYtelseGrunnlag hentGrunnlagForGrunnlagId(Long behandlingId, UUID grunnlagUuid);

    /**
     * Finn grunnlag hvis finnes
     *
     * @param behandlingId
     * @return henter optional aggregat
     */
    Optional<InntektArbeidYtelseGrunnlag> finnGrunnlag(Long behandlingId);

    /**
     *
     * @param behandlingId
     * @return Register inntekt og arbeid (Opprett for å endre eller legge til registeropplysning)
     */
    InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(Long behandlingId);

    /**
     *
     * @param behandlingUuid
     * @return Register inntekt og arbeid (Opprett for å endre eller legge til registeropplysning)
     */
    InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(UUID behandlingUuid, UUID angittReferanse, LocalDateTime angittOpprettetTidspunkt);

    /**
     * @param behandlingId
     * @return Saksbehanldet inntekt og arbeid (Opprett for å endre eller legge til saksbehanldet)
     */
    InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(Long behandlingId);

    /**
     * @param behandlingUuid
     * @return Saksbehanldet inntekt og arbeid (Opprett for å endre eller legge til saksbehanldet)
     */
    InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(UUID behandlingUuid, UUID angittReferanse, LocalDateTime angittOpprettetTidspunkt);

    /**
     * Lagre nytt grunnlag (gitt builder for å generere). Builder bør ikke gjenbrukes etter å ha kalt her.
     *
     * @param behandlingId
     * @param inntektArbeidYtelseAggregatBuilder lagrer ned aggregat (builder bestemmer hvilke del av treet som blir lagret)
     */
    void lagreIayAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder);

    /**
     * @deprecated Denne blir lett misbrukt, siden man antagelig ønsker å gjøre mer enn kun fjerne saksbehandlet versjon. Bruk derfor heller
     *             {@link #lagreIayAggregat(Long, InntektArbeidYtelseAggregatBuilder)} etter du er ferdig med alle endringer du trenger å gjøre
     */
    @Deprecated(forRemoval = true)
    void fjernSaksbehandletVersjon(Long behandlingId);

    /**
     * (async) Lagre nytt grunnlag for Oppgitt Opptjening. Builder bør ikke gjenbrukes etter kall her.
     */
    void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder);

    /**
     * (async) Lagre nytt grunnlag for Overstyrt Oppgitt Opptjening.
     */
    void lagreOverstyrtOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder);

    /**
     * Lagre nytt grunnlag for ArbeidsforholdInformasjon. Builder bør ikke gjenbrukes etter kall her.
     *
     * @param behandlingId - Behandling Id
     * @param aktørId - Aktør Id
     * @param builder - {@link ArbeidsforholdInformasjonBuilder}
     */
    void lagreArbeidsforhold(Long behandlingId, AktørId aktørId, ArbeidsforholdInformasjonBuilder builder);

    /**
     * (async) Kopier IAY grunnlag fra en behandling til en annen.
     *
     * @param fraBehandlingId - Kilde behandling
     * @param tilBehandlingId - Ny behandling
     */
    void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId);

    /**
     * Kopier IAY grunnlag fra en behandling til en annen.
     *
     * @param fraBehandlingId - Kilde behandling
     * @param tilBehandlingId - Ny behandling
     * @param dataset - aggregatene som skal kopieres
     */
    void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId, Set<Dataset> dataset);

    Set<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer);

    Set<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer, AktørId aktørId, FagsakYtelseType ytelseType);

    Optional<OppgittOpptjening> hentKunOverstyrtOppgittOpptjening(Long behandlingId);

    /**
     * Lagre en eller flere inntektsmeldinger på en behandling for en sak.
     *
     * @param saksnummer - Saksnummer
     * @param behandlingId - Behandling Id
     * @param builders - Collection med {@link InntektsmeldingBuilder}
     */
    void lagreInntektsmeldinger(Saksnummer saksnummer, Long behandlingId, Collection<InntektsmeldingBuilder> builders);

    Set<Inntektsmelding> hentInntektsmeldingerSidenRef(Saksnummer saksnummer, Long behandlingId, UUID eksternReferanse);

}
