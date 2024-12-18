package no.nav.ung.sak.web.app.tjenester.kodeverk;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;
import no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.ung.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.ung.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.KonsekvensForYtelsen;
import no.nav.ung.kodeverk.behandling.RevurderingVarslingÅrsak;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.ung.kodeverk.dokument.DokumentTypeId;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.geografisk.Region;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.ung.kodeverk.historikk.HistorikkBegrunnelseType;
import no.nav.ung.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.ung.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.ung.kodeverk.historikk.HistorikkOpplysningType;
import no.nav.ung.kodeverk.historikk.HistorikkResultatType;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.kodeverk.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.ung.kodeverk.medlem.MedlemskapDekningType;
import no.nav.ung.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.ung.kodeverk.medlem.MedlemskapType;
import no.nav.ung.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.ung.kodeverk.person.PersonstatusType;
import no.nav.ung.kodeverk.person.SivilstandType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.kodeverk.uttak.UtenlandsoppholdÅrsak;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;

public record StatiskeKodeverdier(
    @NotNull Set<RelatertYtelseTilstand> relatertYtelseTilstander,
    @NotNull Set<FagsakStatus> fagsakStatuser,
    @NotNull Set<FagsakYtelseType> fagsakYtelseTyper,
    @NotNull Set<BehandlingÅrsakType> behandlingÅrsakTyper,
    @NotNull Set<HistorikkBegrunnelseType> historikkBegrunnelseTyper,
    @NotNull Set<OppgaveÅrsak> oppgaveÅrsaker,
    @NotNull Set<MedlemskapManuellVurderingType> medlemskapManuellVurderingTyper,
    @NotNull Set<BehandlingResultatType> behandlingResultatTyper,
    @NotNull Set<PersonstatusType> personstatusTyper,
    @NotNull Set<Venteårsak> venteårsaker,
    @NotNull Set<BehandlingType> behandlingTyper,
    @NotNull Set<ArbeidType> arbeidTyper,
    @NotNull Set<OpptjeningAktivitetType> opptjeningAktivitetTyper,
    @NotNull Set<RevurderingVarslingÅrsak> revurderingVarslingÅrsaker,
    @NotNull Set<Inntektskategori> inntektskategorier,
    @NotNull Set<AktivitetStatus> aktivitetStatuser,
    @NotNull Set<Arbeidskategori> arbeidskategorier,
    @NotNull Set<Fagsystem> fagsystemer,
    @NotNull Set<SivilstandType> sivilstandTyper,
    @NotNull Set<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
    @NotNull Set<SkjermlenkeType> skjermlenkeTyper,
    @NotNull Set<HistorikkOpplysningType> historikkOpplysningTyper,
    @NotNull Set<HistorikkEndretFeltType> historikkEndretFeltTyper,
    @NotNull Set<HistorikkEndretFeltVerdiType> historikkEndretFeltVerdiTyper,
    @NotNull Set<HistorikkinnslagType> historikkinnslagTyper,
    @NotNull Set<HistorikkAktør> historikkAktører,
    @NotNull Set<HistorikkAvklartSoeknadsperiodeType> historikkAvklartSoeknadsperiodeTyper,
    @NotNull Set<HistorikkResultatType> historikkResultatTyper,
    @NotNull Set<BehandlingStatus> behandlingStatuser,
    @NotNull Set<MedlemskapDekningType> medlemskapDekningTyper,
    @NotNull Set<MedlemskapType> medlemskapTyper,
    @NotNull Set<Avslagsårsak> avslagsårsaker,
    @NotNull Set<KonsekvensForYtelsen> konsekvenserForYtelsen,
    @NotNull Set<VilkårType> vilkårTyper,
    @NotNull Set<VurderArbeidsforholdHistorikkinnslag> vurderArbeidsforholdHistorikkinnslag,
    @NotNull Set<TilbakekrevingVidereBehandling> tilbakekrevingVidereBehandlinger,
    @NotNull Set<VurderÅrsak> vurderingsÅrsaker,
    @NotNull Set<Region> regioner,
    @NotNull Set<Landkoder> landkoder,
    @NotNull Set<Språkkode> språkkoder,
    @NotNull Set<VedtakResultatType> vedtakResultatTyper,
    @NotNull Set<DokumentTypeId> dokumentTypeIder,
    @NotNull Set<UtenlandsoppholdÅrsak> utenlandsoppholdÅrsaker,
    @NotNull Set<ÅrsakTilVurdering> årsakerTilVurdering
) {

    // Eigentleg ikkje så nødvendig, men signaliserer tydleg at dei fleste verdier er enums som implementerer Kodeverdi.
    public static <K extends Enum<K> & Kodeverdi> Set<K> alleEnumVerdier(Class<K> k) {
        return EnumSet.allOf(k);
    }

    public static final StatiskeKodeverdier alle;

    static {
        alle = new StatiskeKodeverdier(
            alleEnumVerdier(RelatertYtelseTilstand.class),
            alleEnumVerdier(FagsakStatus.class),
            alleEnumVerdier(FagsakYtelseType.class),
            alleEnumVerdier(BehandlingÅrsakType.class),
            alleEnumVerdier(HistorikkBegrunnelseType.class),
            alleEnumVerdier(OppgaveÅrsak.class),
            alleEnumVerdier(MedlemskapManuellVurderingType.class).stream().filter(v -> v.visesPåKlient()).collect(Collectors.toSet()),
            alleEnumVerdier(BehandlingResultatType.class),
            alleEnumVerdier(PersonstatusType.class),
            EnumSet.allOf(Venteårsak.class),
            alleEnumVerdier(BehandlingType.class),
            alleEnumVerdier(ArbeidType.class).stream().filter(v -> v.erAnnenOpptjening()).collect(Collectors.toSet()),
            alleEnumVerdier(OpptjeningAktivitetType.class),
            alleEnumVerdier(RevurderingVarslingÅrsak.class),
            alleEnumVerdier(Inntektskategori.class),
            alleEnumVerdier(AktivitetStatus.class),
            alleEnumVerdier(Arbeidskategori.class),
            alleEnumVerdier(Fagsystem.class),
            alleEnumVerdier(SivilstandType.class),
            alleEnumVerdier(FaktaOmBeregningTilfelle.class),
            alleEnumVerdier(SkjermlenkeType.class),
            alleEnumVerdier(HistorikkOpplysningType.class),
            alleEnumVerdier(HistorikkEndretFeltType.class),
            alleEnumVerdier(HistorikkEndretFeltVerdiType.class),
            alleEnumVerdier(HistorikkinnslagType.class),
            alleEnumVerdier(HistorikkAktør.class),
            alleEnumVerdier(HistorikkAvklartSoeknadsperiodeType.class),
            alleEnumVerdier(HistorikkResultatType.class),
            alleEnumVerdier(BehandlingStatus.class),
            alleEnumVerdier(MedlemskapDekningType.class),
            alleEnumVerdier(MedlemskapType.class),
            alleEnumVerdier(Avslagsårsak.class),
            alleEnumVerdier(KonsekvensForYtelsen.class),
            alleEnumVerdier(VilkårType.class),
            alleEnumVerdier(VurderArbeidsforholdHistorikkinnslag.class),
            alleEnumVerdier(TilbakekrevingVidereBehandling.class),
            alleEnumVerdier(VurderÅrsak.class),
            alleEnumVerdier(Region.class),
            new HashSet<>(Landkoder.kodeMap().values()),
            new HashSet<>(Språkkode.kodeMap().values()),
            alleEnumVerdier(VedtakResultatType.class),
            alleEnumVerdier(DokumentTypeId.class),
            alleEnumVerdier(UtenlandsoppholdÅrsak.class),
            alleEnumVerdier(ÅrsakTilVurdering.class)
        );
    }
}
