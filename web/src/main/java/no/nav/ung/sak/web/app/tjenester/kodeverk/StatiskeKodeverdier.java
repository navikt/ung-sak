package no.nav.ung.sak.web.app.tjenester.kodeverk;

import jakarta.validation.constraints.NotNull;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.arbeidsforhold.*;
import no.nav.k9.kodeverk.behandling.*;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.historikk.*;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    @NotNull Set<ArbeidsforholdHandlingType> arbeidsforholdHandlingTyper,
    @NotNull Set<HistorikkOpplysningType> historikkOpplysningTyper,
    @NotNull Set<HistorikkEndretFeltType> historikkEndretFeltTyper,
    @NotNull Set<HistorikkEndretFeltVerdiType>  historikkEndretFeltVerdiTyper,
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
    @NotNull Set<PermisjonsbeskrivelseType> permisjonsbeskrivelseTyper,
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
            alleEnumVerdier(ArbeidsforholdHandlingType.class),
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
            alleEnumVerdier(PermisjonsbeskrivelseType.class),
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
