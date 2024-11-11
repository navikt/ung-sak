package no.nav.ung.sak.web.app.tjenester.kodeverk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.RevurderingVarslingÅrsak;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkBegrunnelseType;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;

/**
 * Denne klassen er definert berre for å eksponere kodeverk og andre typer frå ung-sak java kodebasen ut til k9-sak-web
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
public class KodeverkWeb {
    @NotNull
    @Valid
    public KodeverkWeb.KodeverkArbeidsforhold arbeidsforhold;
    public static class KodeverkArbeidsforhold {
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
    @Valid
    public KodeverkWeb.KodeverkBehandling behandling;
    public static class KodeverkBehandling {
        @NotNull
        public BehandlingÅrsakType behandlingÅrsakType;
        @NotNull
        public BehandlingResultatType behandlingResultatType;
        @NotNull
        public BehandlingType behandlingType;
        @NotNull
        public RevurderingVarslingÅrsak revurderingVarslingÅrsak;


        @NotNull
        @Valid
        public KodeverkBehandlingAksjonspunkt aksjonspunkt;
        public static class KodeverkBehandlingAksjonspunkt {
            @NotNull
            public Venteårsak venteårsak;
        }
    }

    @NotNull
    @Valid
    public KodeverkHistorikk historikk;
    public static class KodeverkHistorikk {
        @NotNull
        public HistorikkBegrunnelseType historikkBegrunnelseType;
    }

    @NotNull
    @Valid
    KodeverkProduksjonsstyring produksjonsstyring;
    public static class KodeverkProduksjonsstyring {
        @NotNull
        public OppgaveÅrsak oppgaveÅrsak;
    }

    @NotNull
    @Valid
    public KodeverkMedlem medlem;
    public static class KodeverkMedlem {
        @NotNull
        public MedlemskapManuellVurderingType medlemskapManuellVurderingType;
    }

    @NotNull
    @Valid
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
    @Valid
    public KodeverkPerson person;
    public static class KodeverkPerson {
        @NotNull
        public PersonstatusType personstatusType;
        @NotNull
        public SivilstandType sivilstandType;
    }

    @NotNull
    @Valid
    public KodeverkOpptjening opptjening;
    public static class KodeverkOpptjening {
        @NotNull
        public OpptjeningAktivitetType opptjeningAktivitetType;
    }

    @NotNull
    @Valid
    public Fagsystem fagsystem;

}
