package no.nav.k9.sak.web.app.tjenester.kodeverk;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;
import no.nav.k9.kodeverk.behandling.RevurderingVarslingÅrsak;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagAndeltype;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.k9.kodeverk.historikk.HistorikkBegrunnelseType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.historikk.HistorikkOpplysningType;
import no.nav.k9.kodeverk.historikk.HistorikkResultatType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@ApplicationScoped
public class HentKodeverkTjeneste {

    public static final Map<String, Collection<?>> KODEVERDIER_SOM_BRUKES_PÅ_KLIENT;
    static {
        Map<String, Collection<?>> map = new LinkedHashMap<>();

        map.put(RelatertYtelseTilstand.class.getSimpleName(), RelatertYtelseTilstand.kodeMap().values());
        map.put(FagsakStatus.class.getSimpleName(), FagsakStatus.kodeMap().values());
        map.put(FagsakYtelseType.class.getSimpleName(), FagsakYtelseType.kodeMap().values());
        map.put(BehandlingÅrsakType.class.getSimpleName(), BehandlingÅrsakType.kodeMap().values());
        map.put(HistorikkBegrunnelseType.class.getSimpleName(), HistorikkBegrunnelseType.kodeMap().values());
        map.put(OppgaveÅrsak.class.getSimpleName(), OppgaveÅrsak.kodeMap().values());
        map.put(MedlemskapManuellVurderingType.class.getSimpleName(), filtrerMedlemskapManuellVurderingType(MedlemskapManuellVurderingType.kodeMap().values()));
        map.put(BehandlingResultatType.class.getSimpleName(), BehandlingResultatType.kodeMap().values());
        map.put(VirksomhetType.class.getSimpleName(), VirksomhetType.kodeMap().values());
        map.put(PersonstatusType.class.getSimpleName(), PersonstatusType.kodeMap().values());
        map.put(FagsakYtelseType.class.getSimpleName(), FagsakYtelseType.kodeMap().values());
        map.put(Venteårsak.class.getSimpleName(), Venteårsak.kodeMap().values());
        map.put(BehandlingType.class.getSimpleName(), BehandlingType.kodeMap().values());
        map.put(ArbeidType.class.getSimpleName(), filtrerArbeidType(ArbeidType.kodeMap().values()));
        map.put(OpptjeningAktivitetType.class.getSimpleName(), OpptjeningAktivitetType.kodeMap().values());
        map.put(RevurderingVarslingÅrsak.class.getSimpleName(), RevurderingVarslingÅrsak.kodeMap().values());
        map.put(Inntektskategori.class.getSimpleName(), Inntektskategori.kodeMap().values());
        map.put(BeregningsgrunnlagAndeltype.class.getSimpleName(), BeregningsgrunnlagAndeltype.kodeMap().values());
        map.put(AktivitetStatus.class.getSimpleName(), AktivitetStatus.kodeMap().values());
        map.put(Arbeidskategori.class.getSimpleName(), Arbeidskategori.kodeMap().values());
        map.put(Fagsystem.class.getSimpleName(), Fagsystem.kodeMap().values());
        map.put(SivilstandType.class.getSimpleName(), SivilstandType.kodeMap().values());
        map.put(FaktaOmBeregningTilfelle.class.getSimpleName(), FaktaOmBeregningTilfelle.kodeMap().values());
        map.put(SkjermlenkeType.class.getSimpleName(), SkjermlenkeType.kodeMap().values());
        map.put(ArbeidsforholdHandlingType.class.getSimpleName(), ArbeidsforholdHandlingType.kodeMap().values());
        map.put(HistorikkOpplysningType.class.getSimpleName(), HistorikkOpplysningType.kodeMap().values());
        map.put(HistorikkEndretFeltType.class.getSimpleName(), HistorikkEndretFeltType.kodeMap().values());
        map.put(HistorikkEndretFeltVerdiType.class.getSimpleName(), HistorikkEndretFeltVerdiType.kodeMap().values());
        map.put(HistorikkinnslagType.class.getSimpleName(), HistorikkinnslagType.kodeMap().values());
        map.put(HistorikkAktør.class.getSimpleName(), HistorikkAktør.kodeMap().values());
        map.put(HistorikkAvklartSoeknadsperiodeType.class.getSimpleName(), HistorikkAvklartSoeknadsperiodeType.kodeMap().values());
        map.put(HistorikkResultatType.class.getSimpleName(), HistorikkResultatType.kodeMap().values());
        map.put(BehandlingStatus.class.getSimpleName(), BehandlingStatus.kodeMap().values());
        map.put(MedlemskapDekningType.class.getSimpleName(), MedlemskapDekningType.kodeMap().values());
        map.put(MedlemskapType.class.getSimpleName(), MedlemskapType.kodeMap().values());
        map.put(Avslagsårsak.class.getSimpleName(), Avslagsårsak.kodeMap().values());
        map.put(KonsekvensForYtelsen.class.getSimpleName(), KonsekvensForYtelsen.kodeMap().values());
        map.put(VilkårType.class.getSimpleName(), VilkårType.kodeMap().values());
        map.put(PermisjonsbeskrivelseType.class.getSimpleName(), PermisjonsbeskrivelseType.kodeMap().values());
        map.put(VurderArbeidsforholdHistorikkinnslag.class.getSimpleName(), VurderArbeidsforholdHistorikkinnslag.kodeMap().values());
        map.put(TilbakekrevingVidereBehandling.class.getSimpleName(), TilbakekrevingVidereBehandling.kodeMap().values());
        map.put(VurderÅrsak.class.getSimpleName(), VurderÅrsak.kodeMap().values());
        map.put(Region.class.getSimpleName(), Region.kodeMap().values());
        map.put(Landkoder.class.getSimpleName(), Landkoder.kodeMap().values());
        map.put(Språkkode.class.getSimpleName(), Språkkode.kodeMap().values());
        map.put(VedtakResultatType.class.getSimpleName(), VedtakResultatType.kodeMap().values());
        map.put(DokumentTypeId.class.getSimpleName(), DokumentTypeId.kodeMap().values());

        Map<String, Collection<?>> mapFiltered = new LinkedHashMap<>();

        map.entrySet().forEach(e -> {
            mapFiltered.put(e.getKey(), e.getValue().stream().filter(f -> !(f instanceof Kodeverdi) || !"-".equals(((Kodeverdi) f).getKode())).collect(Collectors.toSet()));
        });

        KODEVERDIER_SOM_BRUKES_PÅ_KLIENT = Collections.unmodifiableMap(mapFiltered);

    }

    private BehandlendeEnhetTjeneste enhetsTjeneste;

    HentKodeverkTjeneste() {
        // for CDI proxy
    }

    private static Collection<? extends Kodeverdi> filtrerMedlemskapManuellVurderingType(Collection<MedlemskapManuellVurderingType> values) {
        return values.stream().filter(at -> at.visesPåKlient()).collect(Collectors.toSet());
    }

    private static Collection<? extends Kodeverdi> filtrerArbeidType(Collection<ArbeidType> values) {
        return values.stream().filter(at -> at.erAnnenOpptjening()).collect(Collectors.toSet());
    }

    @Inject
    public HentKodeverkTjeneste(BehandlendeEnhetTjeneste enhetsTjeneste) {
        Objects.requireNonNull(enhetsTjeneste, "enhetsTjeneste"); //$NON-NLS-1$
        this.enhetsTjeneste = enhetsTjeneste;
    }

    public Map<String, Collection<?>> hentGruppertKodeliste() {
        // slå sammen kodeverdi og kodeliste maps
        Map<String, Collection<?>> kodelistMap = new LinkedHashMap<>();
        kodelistMap.putAll(KODEVERDIER_SOM_BRUKES_PÅ_KLIENT);

        return kodelistMap;

    }

    public List<OrganisasjonsEnhet> hentBehandlendeEnheter(FagsakYtelseType ytelseType) {
        return enhetsTjeneste.hentEnhetListe(ytelseType);
    }
}
