package no.nav.ung.sak.domene.iay.modell;

import no.nav.abakus.iaygrunnlag.request.Dataset;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface InntektArbeidYtelseTjeneste {


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
     * Lagre nytt grunnlag for Overstyrt Oppgitt Opptjening.
     */
    void lagreOverstyrtOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder);


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

    Optional<OppgittOpptjening> hentKunOverstyrtOppgittOpptjening(Long behandlingId);


}
