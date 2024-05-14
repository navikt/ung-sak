package no.nav.k9.sak.web.app.tjenester.kodeverk;

import jakarta.validation.constraints.NotNull;
import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.*;
import no.nav.k9.kodeverk.behandling.*;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkBegrunnelseType;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;

/**
 * Denne klassen er definert berre for å eksponere kodeverk og andre typer frå k9-sak java kodebasen ut til k9-sak-web
 * typescript gjennom openapi spec kodegenerering.
 *
 * Subklasser er brukt for å representere pakke-inndelinga i java kodebasen
 *
 * Tek utgangspunkt i Kodeverk typer returnert i HentKodeverkTjeneste.hentGruppertKodeliste().
 *
 * Typer som leggast til bør må ha toString() definert til å returnere kode verdi for å få korrekt openapi spec generert
 * frå enum. Eventuelt vil det og fungere viss alle enum name verdier tilfeldigvis er dei samme som tilhøyrande kode verdi.
 *
 * Legger hovedsakleg til typer som ikkje er enkelt tilgjengeleg frå allereie brukte Dto klasser her.
 */
public class K9SakKodeverkWeb {
    @NotNull
    public K9SakKodeverkArbeidsforhold arbeidsforhold;
    public static class K9SakKodeverkArbeidsforhold {
        @NotNull
        public RelatertYtelseTilstand relatertYtelseTilstand;
        @NotNull
        public ArbeidType arbeidType;
        @NotNull
        public Inntektskategori inntektskategori;
        @NotNull
        public AktivitetStatus aktivitetStatus;
        @NotNull
        public Arbeidskategori arbeidskategori;
    }

    @NotNull
    public K9SakKodeverkBehandling behandling;
    public static class K9SakKodeverkBehandling {
        @NotNull
        public BehandlingÅrsakType behandlingÅrsakType;
        @NotNull
        public BehandlingResultatType behandlingResultatType;
        @NotNull
        public BehandlingType behandlingType;
        @NotNull
        public RevurderingVarslingÅrsak revurderingVarslingÅrsak;


        @NotNull
        public K9SakKodeverkBehandlingAksjonspunkt aksjonspunkt;
        public static class K9SakKodeverkBehandlingAksjonspunkt {
            @NotNull
            public Venteårsak venteårsak;
        }
    }

    @NotNull
    public K9SakKodeverkHistorikk historikk;
    public static class K9SakKodeverkHistorikk {
        @NotNull
        public HistorikkBegrunnelseType historikkBegrunnelseType;
    }

    @NotNull K9SakKodeverkProduksjonsstyring produksjonsstyring;
    public static class K9SakKodeverkProduksjonsstyring {
        @NotNull
        public OppgaveÅrsak oppgaveÅrsak;
    }

    @NotNull
    public K9SakKodeverkMedlem medlem;
    public static class K9SakKodeverkMedlem {
        @NotNull
        public MedlemskapManuellVurderingType medlemskapManuellVurderingType;
    }

    @NotNull
    public AbakusKodeverkIaygrunnlag iaygrunnlag;
    public static class AbakusKodeverkIaygrunnlag {
        /**
         * Mangler toString() overstyring for å få kode verdi i generert openapi spec. Men enum konstant namn er like
         * kodeverdien, så bør gå bra.
         */
        @NotNull
        public VirksomhetType virksomhetType;
    }

    @NotNull
    public K9SakKodeverkPerson person;
    public static class K9SakKodeverkPerson {
        @NotNull
        public PersonstatusType personstatusType;
        @NotNull
        public SivilstandType sivilstandType;
    }

    @NotNull
    public K9SakKodeverkOpptjening opptjening;
    public static class K9SakKodeverkOpptjening {
        @NotNull
        public OpptjeningAktivitetType opptjeningAktivitetType;
    }

    @NotNull
    public Fagsystem fagsystem;

}
