package no.nav.ung.sak.domene.arbeidsforhold;

import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
     * @param behandlingId
     * @return Register inntekt og arbeid (Opprett for å endre eller legge til registeropplysning)
     */
    InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(Long behandlingId);

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
     * Lagre nytt grunnlag for Oppgitt Opptjening.
     */
    /**
     * @deprecated (brukes kun i test) Bruk AsyncAbakusLagreOpptjeningTask i modul mottak i stedet
     */
    void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder);


    /**
     * Lagre nytt grunnlag for ArbeidsforholdInformasjon. Builder bør ikke gjenbrukes etter kall her.
     *
     * @param behandlingId - Behandling Id
     * @param aktørId      - Aktør Id
     * @param builder      - {@link ArbeidsforholdInformasjonBuilder}
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
     * @param dataset         - aggregatene som skal kopieres
     */
    void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId, Set<Dataset> dataset);
}
